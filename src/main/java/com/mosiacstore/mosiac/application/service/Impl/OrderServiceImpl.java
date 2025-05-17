package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.UserDto;
import com.mosiacstore.mosiac.application.dto.request.CheckoutRequest;
import com.mosiacstore.mosiac.application.dto.request.OrderItemRequest;
import com.mosiacstore.mosiac.application.dto.request.UpdateOrderItemsRequest;
import com.mosiacstore.mosiac.application.dto.response.*;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.InvalidOperationException;
import com.mosiacstore.mosiac.application.service.AdminNotificationService;
import com.mosiacstore.mosiac.application.service.OrderService;
import com.mosiacstore.mosiac.application.service.PaymentService;
import com.mosiacstore.mosiac.domain.address.Address;
import com.mosiacstore.mosiac.domain.cart.Cart;
import com.mosiacstore.mosiac.domain.cart.CartItem;
import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.domain.order.OrderItem;
import com.mosiacstore.mosiac.domain.order.OrderStatus;
import com.mosiacstore.mosiac.domain.payment.Payment;
import com.mosiacstore.mosiac.domain.payment.PaymentMethod;
import com.mosiacstore.mosiac.domain.payment.PaymentStatus;
import com.mosiacstore.mosiac.domain.product.Product;
import com.mosiacstore.mosiac.domain.product.ProductVariant;
import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.infrastructure.repository.*;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final AdminNotificationService adminNotificationService;
    private final EmailService emailService;

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000"); // 500,000 VND
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("30000"); // 30,000 VND

    @Override
    public PageResponse<OrderResponse> getUserOrders(UUID userId, int page, int size, String sort) {
        Sort sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isEmpty()) {
            String[] params = sort.split(",");
            Sort.Direction dir = params.length > 1 && params[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sorting = Sort.by(dir, params[0]);
        }

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        List<OrderResponse> orderResponses = orderPage.getContent().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                orderResponses,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isFirst(),
                orderPage.isLast()
        );
    }

    @Override
    public OrderResponse getOrderById(UUID id, UUID userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        if (!order.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Order does not belong to the current user");
        }

        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse getOrderByNumber(String orderNumber, UUID userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with number: " + orderNumber));

        if (!order.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Order does not belong to the current user");
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request, UUID userId) {
        // Fetch user and cart
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user with ID: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new InvalidOperationException("Cannot checkout with empty cart");
        }

        // Validate shipping address
        Address shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .orElseThrow(() -> new EntityNotFoundException("Shipping address not found with ID: " + request.getShippingAddressId()));

        if (!shippingAddress.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Shipping address does not belong to the current user");
        }

        // Validate payment method
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOperationException("Invalid payment method: " + request.getPaymentMethod());
        }

        // Calculate totals
        BigDecimal totalProductAmount = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal itemPrice = item.getPriceSnapshot().multiply(new BigDecimal(item.getQuantity()));
            totalProductAmount = totalProductAmount.add(itemPrice);

            // Check stock availability
            Integer stockQuantity;
            if (item.getVariant() != null) {
                stockQuantity = item.getVariant().getStockQuantity();
            } else {
                stockQuantity = item.getProduct().getStockQuantity();
            }

            if (stockQuantity != null && stockQuantity < item.getQuantity()) {
                throw new InvalidOperationException("Not enough stock for product: " + item.getProduct().getName());
            }
        }

        // Calculate shipping fee (free shipping over threshold)
        BigDecimal shippingFee = totalProductAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : STANDARD_SHIPPING_FEE;

        BigDecimal totalAmount = totalProductAmount.add(shippingFee);

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalProductAmount(totalProductAmount);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(totalAmount);
        order.setRecipientName(shippingAddress.getRecipientName());
        order.setRecipientPhone(shippingAddress.getPhone());
        order.setShippingAddress(shippingAddress);

        // Create address snapshot
        String addressSnapshot = String.format("%s, %s, %s, %s",
                shippingAddress.getStreetAddress(),
                shippingAddress.getWard().getName(),
                shippingAddress.getDistrict().getName(),
                shippingAddress.getProvince().getName());
        order.setShippingAddressSnapshot(addressSnapshot);

        order.setNote(request.getNote());
        order.setPaymentDue(LocalDateTime.now().plusDays(2)); // Payment due in 2 days
        order.setOrderItems(new HashSet<>());
        order.setPayments(new HashSet<>());

        Order savedOrder = orderRepository.save(order);

        // Create order items
        Set<OrderItem> orderItems = new HashSet<>();
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setVariant(cartItem.getVariant());
            orderItem.setProductNameSnapshot(cartItem.getProduct().getName());

            // Create variant snapshot
            if (cartItem.getVariant() != null) {
                ProductVariant variant = cartItem.getVariant();
                String variantInfo = String.format("Size: %s%s",
                        variant.getSize(),
                        variant.getColor() != null ? ", Color: " + variant.getColor() : "");
                orderItem.setVariantInfoSnapshot(variantInfo);
            }

            orderItem.setPriceSnapshot(cartItem.getPriceSnapshot());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(cartItem.getPriceSnapshot().multiply(new BigDecimal(cartItem.getQuantity())));

            OrderItem savedOrderItem = orderItemRepository.save(orderItem);
            orderItems.add(savedOrderItem);

            // Update product stock
            if (cartItem.getVariant() != null) {
                ProductVariant variant = cartItem.getVariant();
                if (variant.getStockQuantity() != null) {
                    variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
                    productRepository.save(cartItem.getProduct());
                }
            } else {
                if (cartItem.getProduct().getStockQuantity() != null) {
                    cartItem.getProduct().setStockQuantity(
                            cartItem.getProduct().getStockQuantity() - cartItem.getQuantity());
                    productRepository.save(cartItem.getProduct());
                }
            }
        }

        savedOrder.setOrderItems(orderItems);

        // Create payment record
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(totalAmount);

        if (paymentMethod == PaymentMethod.COD) {
            // For COD, we immediately set status to PROCESSING
            payment.setStatus(PaymentStatus.PENDING);
            savedOrder.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(savedOrder);
        } else {
            payment.setStatus(PaymentStatus.PENDING);
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Clear the cart
        cart.getItems().clear();
        cartRepository.save(cart);

        OrderResponse orderResponse = mapToOrderResponse(savedOrder);

        // Create checkout response with payment instructions
        CheckoutResponse checkoutResponse = new CheckoutResponse();
        checkoutResponse.setOrder(orderResponse);

        if (paymentMethod == PaymentMethod.BANK_TRANSFER) {
            checkoutResponse.setPaymentInstructions(
                    "Please transfer the exact amount to the following bank account:\n" +
                            "Bank: MOMO\n" +
                            "Account Number: 0788732514\n" +
                            "Account Name: MOSAIC STORE\n" +
                            "Amount: " + totalAmount + " VND\n" +
                            "Reference: " + savedOrder.getOrderNumber());
        }

        // Send admin notification asynchronously
        adminNotificationService.notifyNewOrder(savedOrder);

        // Send confirmation email to customer
        emailService.sendOrderConfirmationEmail(savedOrder);

        return checkoutResponse;
    }


    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID id, String reason, UUID userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        if (!order.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Order does not belong to the current user");
        }

        // Check if order can be cancelled
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOperationException("Cannot cancel a delivered order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Order is already cancelled");
        }

        // For shipping orders, we may need additional validation or admin approval
        if (order.getStatus() == OrderStatus.SHIPPING) {
            throw new InvalidOperationException("Cannot cancel an order that is already being shipped. Please contact customer support.");
        }

        // Update order status
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason);

        // Restore product stock
        for (OrderItem item : order.getOrderItems()) {
            if (item.getVariant() != null) {
                ProductVariant variant = item.getVariant();
                if (variant.getStockQuantity() != null) {
                    variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                }
            } else {
                if (item.getProduct().getStockQuantity() != null) {
                    item.getProduct().setStockQuantity(
                            item.getProduct().getStockQuantity() + item.getQuantity());
                }
            }
            productRepository.save(item.getProduct());
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToOrderResponse(updatedOrder);
    }

    @Override
    public PageResponse<OrderResponse> getAllOrders(String keyword, String status, UUID userId,
                                                    LocalDateTime startDate, LocalDateTime endDate,
                                                    int page, int size, String sort) {
        // Create specification for filtering
        Specification<Order> spec = Specification.where(null);

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(root.get("orderNumber"), "%" + keyword + "%"),
                    cb.like(root.get("recipientName"), "%" + keyword + "%"),
                    cb.like(root.get("recipientPhone"), "%" + keyword + "%")
            ));
        }

        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), orderStatus));
            } catch (IllegalArgumentException ignored) {
                // Invalid status, ignore this filter
            }
        }

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        }

        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }

        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        Sort sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isEmpty()) {
            String[] params = sort.split(",");
            Sort.Direction dir = params.length > 1 && params[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sorting = Sort.by(dir, params[0]);
        }

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        List<OrderResponse> orderResponses = orderPage.getContent().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                orderResponses,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isFirst(),
                orderPage.isLast()
        );
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID id, String status, String adminNote, UUID adminId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOperationException("Invalid order status: " + status);
        }

        // Validate status transition
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        order.setAdminNote(adminNote);
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        if (oldStatus != newStatus) {
            switch (newStatus) {
                case PAID:
                    emailService.sendOrderPaidEmail(updatedOrder);
                    break;
                case PROCESSING:
                    emailService.sendOrderProcessingEmail(updatedOrder);
                    break;
                case SHIPPING:
                    emailService.sendOrderShippingEmail(updatedOrder);
                    break;
                case DELIVERED:
                    emailService.sendOrderDeliveredEmail(updatedOrder);
                    break;
                case CANCELLED:
                    emailService.sendOrderCancelledEmail(updatedOrder);
                    break;
                default:
                    break;
            }
        }

        return mapToOrderResponse(updatedOrder);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        Set<OrderStatus> allowedTransitions;
        switch (currentStatus) {
            case PENDING_PAYMENT:
                allowedTransitions = Set.of(OrderStatus.PAID, OrderStatus.CANCELLED);
                break;
            case PAID:
                allowedTransitions = Set.of(OrderStatus.PROCESSING);
                break;
            case PROCESSING:
                allowedTransitions = Set.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED);
                break;
            case SHIPPING:
                allowedTransitions = Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
                break;
            case DELIVERED:
                allowedTransitions = Set.of(OrderStatus.CANCELLED); // Có thể thêm logic trả hàng nếu cần
                break;
            case CANCELLED:
                allowedTransitions = Set.of();
                break;
            default:
                throw new InvalidOperationException("Trạng thái hiện tại không hợp lệ: " + currentStatus);
        }
        if (!allowedTransitions.contains(newStatus)) {
            throw new InvalidOperationException("Không thể chuyển từ " + currentStatus + " sang " + newStatus);
        }
    }

    private String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String datePrefix = LocalDateTime.now().format(formatter);

        // Generate random 4-digit number
        Random random = new Random();
        int randomNum = 1000 + random.nextInt(9000);

        return "VS" + datePrefix + randomNum;
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> orderItemResponses = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        // Get payment information if exists
        PaymentResponse paymentResponse = null;
        if (!order.getPayments().isEmpty()) {
            Payment payment = order.getPayments().iterator().next();
            paymentResponse = mapToPaymentResponse(payment);
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalProductAmount(order.getTotalProductAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddressSnapshot(order.getShippingAddressSnapshot())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .orderItems(orderItemResponses)
                .payment(paymentResponse)
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .productNameSnapshot(orderItem.getProductNameSnapshot())
                .variantInfoSnapshot(orderItem.getVariantInfoSnapshot())
                .priceSnapshot(orderItem.getPriceSnapshot())
                .quantity(orderItem.getQuantity())
                .subtotal(orderItem.getSubtotal())
                .build();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionReference(payment.getTransactionReference())
                .paymentDate(payment.getPaymentDate())
                .bankName(payment.getBankName())
                .bankAccountNumber(payment.getBankAccountNumber())
                .paymentNote(payment.getPaymentNote())
                .build();
    }

    @Override
    public OrderDetailResponse getOrderDetails(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        return mapToOrderDetailResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderItems(UUID id, UpdateOrderItemsRequest request, UUID adminId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        // Validate order status
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot update items for delivered or cancelled orders");
        }

        // First, remove all existing items
        orderItemRepository.deleteAll(order.getOrderItems());
        order.getOrderItems().clear();

        // Then add the new items
        BigDecimal totalProductAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + itemRequest.getProductId()));

            ProductVariant variant = null;
            if (itemRequest.getVariantId() != null) {
                variant = variantRepository.findById(itemRequest.getVariantId())
                        .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + itemRequest.getVariantId()));
            }

            BigDecimal price;
            if (itemRequest.getPriceOverride() != null) {
                price = itemRequest.getPriceOverride();
            } else if (variant != null && variant.getPriceAdjustment() != null) {
                price = product.getPrice().add(variant.getPriceAdjustment());
            } else {
                price = product.getPrice();
            }

            BigDecimal subtotal = price.multiply(new BigDecimal(itemRequest.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setProductNameSnapshot(product.getName());

            if (variant != null) {
                String variantInfo = String.format("Size: %s%s",
                        variant.getSize(),
                        variant.getColor() != null ? ", Color: " + variant.getColor() : "");
                orderItem.setVariantInfoSnapshot(variantInfo);
            }

            orderItem.setPriceSnapshot(price);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setSubtotal(subtotal);

            OrderItem savedOrderItem = orderItemRepository.save(orderItem);
            order.getOrderItems().add(savedOrderItem);

            totalProductAmount = totalProductAmount.add(subtotal);
        }

        // Recalculate order totals
        order.setTotalProductAmount(totalProductAmount);

        // Recalculate shipping fee if applicable
        BigDecimal shippingFee = totalProductAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : STANDARD_SHIPPING_FEE;
        order.setShippingFee(shippingFee);

        order.setTotalAmount(totalProductAmount.add(shippingFee));
        order.setUpdatedAt(LocalDateTime.now());

        // Update payment amount if exists
        if (!order.getPayments().isEmpty()) {
            Payment payment = order.getPayments().iterator().next();
            payment.setAmount(order.getTotalAmount());
            paymentRepository.save(payment);
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse addOrderItem(UUID id, OrderItemRequest request, UUID adminId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        // Validate order status
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot add items to delivered or cancelled orders");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + request.getProductId()));

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + request.getVariantId()));
        }

        BigDecimal price;
        if (request.getPriceOverride() != null) {
            price = request.getPriceOverride();
        } else if (variant != null && variant.getPriceAdjustment() != null) {
            price = product.getPrice().add(variant.getPriceAdjustment());
        } else {
            price = product.getPrice();
        }

        BigDecimal subtotal = price.multiply(new BigDecimal(request.getQuantity()));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setVariant(variant);
        orderItem.setProductNameSnapshot(product.getName());

        if (variant != null) {
            String variantInfo = String.format("Size: %s%s",
                    variant.getSize(),
                    variant.getColor() != null ? ", Color: " + variant.getColor() : "");
            orderItem.setVariantInfoSnapshot(variantInfo);
        }

        orderItem.setPriceSnapshot(price);
        orderItem.setQuantity(request.getQuantity());
        orderItem.setSubtotal(subtotal);

        OrderItem savedOrderItem = orderItemRepository.save(orderItem);
        order.getOrderItems().add(savedOrderItem);

        // Recalculate order totals
        BigDecimal totalProductAmount = order.getTotalProductAmount().add(subtotal);
        order.setTotalProductAmount(totalProductAmount);

        // Recalculate shipping fee if applicable
        BigDecimal shippingFee = totalProductAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : STANDARD_SHIPPING_FEE;
        order.setShippingFee(shippingFee);

        order.setTotalAmount(totalProductAmount.add(shippingFee));
        order.setUpdatedAt(LocalDateTime.now());

        // Update payment amount if exists
        if (!order.getPayments().isEmpty()) {
            Payment payment = order.getPayments().iterator().next();
            payment.setAmount(order.getTotalAmount());
            paymentRepository.save(payment);
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse removeOrderItem(UUID id, UUID itemId, UUID adminId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        // Validate order status
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot remove items from delivered or cancelled orders");
        }

        OrderItem itemToRemove = null;
        for (OrderItem item : order.getOrderItems()) {
            if (item.getId().equals(itemId)) {
                itemToRemove = item;
                break;
            }
        }

        if (itemToRemove == null) {
            throw new EntityNotFoundException("Order item not found with ID: " + itemId);
        }

        BigDecimal subtotalToRemove = itemToRemove.getSubtotal();

        // Remove the item
        order.getOrderItems().remove(itemToRemove);
        orderItemRepository.delete(itemToRemove);

        // Recalculate order totals
        BigDecimal totalProductAmount = order.getTotalProductAmount().subtract(subtotalToRemove);
        order.setTotalProductAmount(totalProductAmount);

        // Recalculate shipping fee if applicable
        BigDecimal shippingFee = totalProductAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : STANDARD_SHIPPING_FEE;
        order.setShippingFee(shippingFee);

        order.setTotalAmount(totalProductAmount.add(shippingFee));
        order.setUpdatedAt(LocalDateTime.now());

        // Update payment amount if exists
        if (!order.getPayments().isEmpty()) {
            Payment payment = order.getPayments().iterator().next();
            payment.setAmount(order.getTotalAmount());
            paymentRepository.save(payment);
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToOrderResponse(updatedOrder);
    }

    @Override
    public Resource exportOrders(String format, String status, LocalDateTime startDate, LocalDateTime endDate) {
        // Set default dates if not provided
        LocalDateTime start = startDate != null ? startDate : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Create specification for date range
        Specification<Order> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("createdAt"), start, end));

        // Add filter for status if provided
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), orderStatus));
            } catch (IllegalArgumentException ignored) {
                // Invalid status, ignore this filter
            }
        }

        // Get all orders matching the criteria
        List<Order> orders = orderRepository.findAll(spec);

        // Generate export file based on format
        if ("csv".equalsIgnoreCase(format)) {
            return generateCsvExport(orders);
        } else if ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
            return generateExcelExport(orders);
        } else {
            // Default to CSV
            return generateCsvExport(orders);
        }
    }

// Helper methods for the new functionality

    private OrderDetailResponse mapToOrderDetailResponse(Order order) {
        List<OrderItemResponse> orderItemResponses = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        // Get payment information if exists
        PaymentResponse paymentResponse = null;
        if (!order.getPayments().isEmpty()) {
            Payment payment = order.getPayments().iterator().next();
            paymentResponse = mapToPaymentResponse(payment);
        }

        // Get invoice information if exists
        InvoiceResponse invoiceResponse = null;
        if (order.getInvoice() != null) {
            invoiceResponse = InvoiceResponse.builder()
                    .id(order.getInvoice().getId())
                    .orderId(order.getId())
                    .invoiceNumber(order.getInvoice().getInvoiceNumber())
                    .pdfUrl(order.getInvoice().getPdfUrl())
                    .issuedDate(order.getInvoice().getIssuedDate())
                    .sent(order.getInvoice().getSent())
                    .build();
        }

        // Map user data
        UserDto userDto = null;
        if (order.getUser() != null) {
            userDto = UserDto.builder()
                    .id(order.getUser().getId())
                    .email(order.getUser().getEmail())
                    .phoneNumber(order.getUser().getPhoneNumber())
                    .fullName(order.getUser().getFullName())
                    .role(order.getUser().getRole())
                    .build();
        }

        return OrderDetailResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalProductAmount(order.getTotalProductAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddressSnapshot(order.getShippingAddressSnapshot())
                .note(order.getNote())
                .adminNote(order.getAdminNote())
                .paymentDue(order.getPaymentDue())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .orderItems(orderItemResponses)
                .payment(paymentResponse)
                .invoice(invoiceResponse)
                .user(userDto)
                .build();
    }

    private Resource generateCsvExport(List<Order> orders) {
        try {
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);

            // Write header
            String[] header = {
                    "Order Number", "Status", "Customer Name", "Phone", "Address",
                    "Total Product Amount", "Shipping Fee", "Total Amount",
                    "Payment Method", "Payment Status", "Created At", "Updated At", "Note"
            };
            csvWriter.writeNext(header);

            // Write data
            for (Order order : orders) {
                String paymentMethod = "";
                String paymentStatus = "";

                if (!order.getPayments().isEmpty()) {
                    Payment payment = order.getPayments().iterator().next();
                    paymentMethod = payment.getPaymentMethod().name();
                    paymentStatus = payment.getStatus().name();
                }

                String[] row = {
                        order.getOrderNumber(),
                        order.getStatus().name(),
                        order.getRecipientName(),
                        order.getRecipientPhone(),
                        order.getShippingAddressSnapshot(),
                        order.getTotalProductAmount().toString(),
                        order.getShippingFee().toString(),
                        order.getTotalAmount().toString(),
                        paymentMethod,
                        paymentStatus,
                        order.getCreatedAt().toString(),
                        order.getUpdatedAt().toString(),
                        order.getNote() != null ? order.getNote() : ""
                };

                csvWriter.writeNext(row);
            }

            csvWriter.close();

            // Convert to resource
            byte[] bytes = stringWriter.toString().getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(bytes);

            return resource;
        } catch (Exception e) {
            log.error("Error generating CSV export", e);
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }

    private Resource generateExcelExport(List<Order> orders) {
        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Orders");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Order Number", "Status", "Customer Name", "Phone", "Address",
                    "Total Product Amount", "Shipping Fee", "Total Amount",
                    "Payment Method", "Payment Status", "Created At", "Updated At", "Note"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Create data rows
            int rowNum = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowNum++);

                String paymentMethod = "";
                String paymentStatus = "";

                if (!order.getPayments().isEmpty()) {
                    Payment payment = order.getPayments().iterator().next();
                    paymentMethod = payment.getPaymentMethod().name();
                    paymentStatus = payment.getStatus().name();
                }

                row.createCell(0).setCellValue(order.getOrderNumber());
                row.createCell(1).setCellValue(order.getStatus().name());
                row.createCell(2).setCellValue(order.getRecipientName());
                row.createCell(3).setCellValue(order.getRecipientPhone());
                row.createCell(4).setCellValue(order.getShippingAddressSnapshot());
                row.createCell(5).setCellValue(order.getTotalProductAmount().doubleValue());
                row.createCell(6).setCellValue(order.getShippingFee().doubleValue());
                row.createCell(7).setCellValue(order.getTotalAmount().doubleValue());
                row.createCell(8).setCellValue(paymentMethod);
                row.createCell(9).setCellValue(paymentStatus);
                row.createCell(10).setCellValue(order.getCreatedAt().toString());
                row.createCell(11).setCellValue(order.getUpdatedAt().toString());
                row.createCell(12).setCellValue(order.getNote() != null ? order.getNote() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            // Convert to resource
            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            return resource;
        } catch (Exception e) {
            log.error("Error generating Excel export", e);
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }
}