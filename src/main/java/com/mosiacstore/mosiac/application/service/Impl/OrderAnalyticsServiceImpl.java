package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.analytics.*;
import com.mosiacstore.mosiac.application.service.OrderAnalyticsService;
import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.domain.order.OrderItem;
import com.mosiacstore.mosiac.domain.order.OrderStatus;
import com.mosiacstore.mosiac.domain.payment.PaymentMethod;
import com.mosiacstore.mosiac.infrastructure.repository.OrderItemRepository;
import com.mosiacstore.mosiac.infrastructure.repository.OrderRepository;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAnalyticsServiceImpl implements OrderAnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Override
    public RevenueAnalyticsResponse getRevenueAnalytics(String period, LocalDateTime startDate, LocalDateTime endDate) {
        // Set default dates if not provided
        LocalDateTime start = startDate != null ? startDate : LocalDate.now().minusMonths(3).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Create specification for date range
        Specification<Order> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("createdAt"), start, end));

        // Add filter for only completed orders
        spec = spec.and((root, query, cb) ->
                cb.or(
                        cb.equal(root.get("status"), OrderStatus.DELIVERED),
                        cb.equal(root.get("status"), OrderStatus.PAID),
                        cb.equal(root.get("status"), OrderStatus.PROCESSING),
                        cb.equal(root.get("status"), OrderStatus.SHIPPING)
                ));

        // Get all orders in range
        List<Order> orders = orderRepository.findAll(spec);

        // Generate time series data
        List<TimeSeriesDataPoint> timeSeriesData = generateTimeSeriesData(
                orders,
                period != null ? period : "daily",
                start,
                end,
                Order::getTotalAmount
        );

        // Calculate statistics
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageRevenue = orders.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(new BigDecimal(orders.size()), 2, RoundingMode.HALF_UP);

        BigDecimal minRevenue = timeSeriesData.stream()
                .map(TimeSeriesDataPoint::getValue)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxRevenue = timeSeriesData.stream()
                .map(TimeSeriesDataPoint::getValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Calculate growth percentage
        double growthPercentage = calculateGrowthPercentage(timeSeriesData);

        return RevenueAnalyticsResponse.builder()
                .data(timeSeriesData)
                .totalRevenue(totalRevenue)
                .averageRevenue(averageRevenue)
                .minRevenue(minRevenue)
                .maxRevenue(maxRevenue)
                .growthPercentage(growthPercentage)
                .build();
    }

    @Override
    public OrderCountAnalyticsResponse getOrderCountAnalytics(String period, LocalDateTime startDate, LocalDateTime endDate) {
        // Set default dates if not provided
        LocalDateTime start = startDate != null ? startDate : LocalDate.now().minusMonths(3).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Create specification for date range
        Specification<Order> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("createdAt"), start, end));

        // Get all orders in range
        List<Order> orders = orderRepository.findAll(spec);

        // Generate time series data for order counts
        List<TimeSeriesDataPoint> timeSeriesData = generateTimeSeriesData(
                orders,
                period != null ? period : "daily",
                start,
                end,
                order -> BigDecimal.ONE
        );

        // Calculate statistics
        long totalOrderCount = orders.size();

        double averageOrderCount = timeSeriesData.stream()
                .mapToLong(TimeSeriesDataPoint::getCount)
                .average()
                .orElse(0);

        long minOrderCount = timeSeriesData.stream()
                .mapToLong(TimeSeriesDataPoint::getCount)
                .min()
                .orElse(0);

        long maxOrderCount = timeSeriesData.stream()
                .mapToLong(TimeSeriesDataPoint::getCount)
                .max()
                .orElse(0);

        // Calculate growth percentage
        double growthPercentage = calculateGrowthPercentage(timeSeriesData);

        return OrderCountAnalyticsResponse.builder()
                .data(timeSeriesData)
                .totalOrderCount(totalOrderCount)
                .averageOrderCount(averageOrderCount)
                .minOrderCount(minOrderCount)
                .maxOrderCount(maxOrderCount)
                .growthPercentage(growthPercentage)
                .build();
    }

    @Override
    public AverageOrderValueResponse getAverageOrderValue(String period, LocalDateTime startDate, LocalDateTime endDate) {
        // Set default dates if not provided
        LocalDateTime start = startDate != null ? startDate : LocalDate.now().minusMonths(3).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Create specification for date range
        Specification<Order> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("createdAt"), start, end));

        // Add filter for only completed orders
        spec = spec.and((root, query, cb) ->
                cb.or(
                        cb.equal(root.get("status"), OrderStatus.DELIVERED),
                        cb.equal(root.get("status"), OrderStatus.PAID),
                        cb.equal(root.get("status"), OrderStatus.PROCESSING),
                        cb.equal(root.get("status"), OrderStatus.SHIPPING)
                ));

        // Get all orders in range
        List<Order> orders = orderRepository.findAll(spec);

        // Generate time series data with average values
        List<TimeSeriesDataPoint> timeSeriesData = generateAverageOrderValueSeries(
                orders,
                period != null ? period : "daily",
                start,
                end
        );

        // Calculate overall average
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal overallAverageValue = orders.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(new BigDecimal(orders.size()), 2, RoundingMode.HALF_UP);

        BigDecimal minAverageValue = timeSeriesData.stream()
                .map(TimeSeriesDataPoint::getValue)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxAverageValue = timeSeriesData.stream()
                .map(TimeSeriesDataPoint::getValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Calculate growth percentage
        double growthPercentage = calculateGrowthPercentage(timeSeriesData);

        return AverageOrderValueResponse.builder()
                .data(timeSeriesData)
                .overallAverageValue(overallAverageValue)
                .minAverageValue(minAverageValue)
                .maxAverageValue(maxAverageValue)
                .growthPercentage(growthPercentage)
                .build();
    }

    @Override
    public List<TopProductResponse> getTopProducts(int limit, LocalDateTime startDate, LocalDateTime endDate) {
        // Set default dates if not provided
        LocalDateTime start = startDate != null ? startDate : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Create specification for date range
        Specification<Order> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("createdAt"), start, end));

        // Add filter for only completed orders
        spec = spec.and((root, query, cb) ->
                cb.or(
                        cb.equal(root.get("status"), OrderStatus.DELIVERED),
                        cb.equal(root.get("status"), OrderStatus.PAID),
                        cb.equal(root.get("status"), OrderStatus.PROCESSING),
                        cb.equal(root.get("status"), OrderStatus.SHIPPING)
                ));

        // Get all orders in range
        List<Order> orders = orderRepository.findAll(spec);

        // Extract all order items
        List<OrderItem> orderItems = orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .collect(Collectors.toList());

        // Group by product and calculate totals
        Map<UUID, TopProductBuilder> productAggregates = new HashMap<>();

        for (OrderItem item : orderItems) {
            if (item.getProduct() == null) continue;

            UUID productId = item.getProduct().getId();
            TopProductBuilder builder = productAggregates.getOrDefault(
                    productId,
                    new TopProductBuilder(
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getProduct().getSlug(),
                            item.getProduct().getImages().stream()
                                    .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                                    .findFirst()
                                    .map(image -> image.getImageUrl())
                                    .orElse(null),
                            item.getProduct().getCategory() != null ? item.getProduct().getCategory().getName() : "Unknown",
                            item.getProduct().getRegion() != null ? item.getProduct().getRegion().getName() : "Unknown"
                    )
            );

            builder.addItem(item.getQuantity(), item.getSubtotal());
            productAggregates.put(productId, builder);
        }

        // Convert to response objects and sort by revenue
        return productAggregates.values().stream()
                .map(TopProductBuilder::build)
                .sorted(Comparator.comparing(TopProductResponse::getRevenue).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<GeographicSalesResponse> getGeographicSales(LocalDateTime startDate, LocalDateTime endDate) {
        // Set default dates if not provided
        LocalDateTime start = startDate != null ? startDate : LocalDate.now().minusMonths(3).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Create specification for date range
        Specification<Order> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("createdAt"), start, end));

        // Add filter for only completed orders
        spec = spec.and((root, query, cb) ->
                cb.or(
                        cb.equal(root.get("status"), OrderStatus.DELIVERED),
                        cb.equal(root.get("status"), OrderStatus.PAID),
                        cb.equal(root.get("status"), OrderStatus.PROCESSING),
                        cb.equal(root.get("status"), OrderStatus.SHIPPING)
                ));

        // Get all orders in range
        List<Order> orders = orderRepository.findAll(spec);

        // Extract address information
        Map<String, GeographicSalesBuilder> provinceSales = new HashMap<>();

        for (Order order : orders) {
            if (order.getShippingAddress() == null || order.getShippingAddress().getProvince() == null) continue;

            String provinceCode = order.getShippingAddress().getProvince().getCode();
            String provinceName = order.getShippingAddress().getProvince().getName();

            GeographicSalesBuilder builder = provinceSales.getOrDefault(
                    provinceCode,
                    new GeographicSalesBuilder(provinceCode, provinceName)
            );

            builder.addOrder(order.getTotalAmount());
            provinceSales.put(provinceCode, builder);
        }

        // Calculate totals
        long totalOrderCount = orders.size();
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build response objects with percentages
        return provinceSales.values().stream()
                .map(builder -> builder.build(totalOrderCount, totalRevenue))
                .sorted(Comparator.comparing(GeographicSalesResponse::getRevenue).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentMethodBreakdownResponse> getPaymentMethodBreakdown(LocalDateTime startDate, LocalDateTime endDate) {
        // Set default dates if not provided
        LocalDateTime start = startDate != null ? startDate : LocalDate.now().minusMonths(3).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Create specification for date range
        Specification<Order> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("createdAt"), start, end));

        // Get all orders in range
        List<Order> orders = orderRepository.findAll(spec);

        // Extract and group by payment method
        Map<PaymentMethod, PaymentMethodBreakdownBuilder> methodBreakdown = new HashMap<>();

        for (Order order : orders) {
            if (order.getPayments() == null || order.getPayments().isEmpty()) continue;

            PaymentMethod method = order.getPayments().iterator().next().getPaymentMethod();

            PaymentMethodBreakdownBuilder builder = methodBreakdown.getOrDefault(
                    method,
                    new PaymentMethodBreakdownBuilder(
                            method,
                            getPaymentMethodDisplayName(method)
                    )
            );

            builder.addOrder(order.getTotalAmount());
            methodBreakdown.put(method, builder);
        }

        // Calculate totals
        long totalOrderCount = orders.size();
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build response objects with percentages
        return methodBreakdown.values().stream()
                .map(builder -> builder.build(totalOrderCount, totalRevenue))
                .sorted(Comparator.comparing(PaymentMethodBreakdownResponse::getRevenue).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public DashboardStatsResponse getDashboardStats() {
        // Current date info
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = today.withDayOfMonth(1);

        // Previous period for comparison
        LocalDate yesterday = today.minusDays(1);
        LocalDate previousWeekStart = startOfWeek.minusWeeks(1);
        LocalDate previousMonthStart = today.minusMonths(1).withDayOfMonth(1);

        // Get orders from repository
        List<Order> allOrders = orderRepository.findAll();
        List<Order> todayOrders = getOrdersInDateRange(allOrders, today.atStartOfDay(), now);
        List<Order> thisWeekOrders = getOrdersInDateRange(allOrders, startOfWeek.atStartOfDay(), now);
        List<Order> thisMonthOrders = getOrdersInDateRange(allOrders, startOfMonth.atStartOfDay(), now);

        List<Order> yesterdayOrders = getOrdersInDateRange(allOrders, yesterday.atStartOfDay(), yesterday.plusDays(1).atStartOfDay());
        List<Order> previousWeekOrders = getOrdersInDateRange(allOrders, previousWeekStart.atStartOfDay(), startOfWeek.atStartOfDay());
        List<Order> previousMonthOrders = getOrdersInDateRange(allOrders, previousMonthStart.atStartOfDay(), startOfMonth.atStartOfDay());

        // Calculate revenue metrics
        BigDecimal totalRevenue = sumOrderRevenue(allOrders);
        BigDecimal todayRevenue = sumOrderRevenue(todayOrders);
        BigDecimal thisWeekRevenue = sumOrderRevenue(thisWeekOrders);
        BigDecimal thisMonthRevenue = sumOrderRevenue(thisMonthOrders);

        BigDecimal yesterdayRevenue = sumOrderRevenue(yesterdayOrders);
        BigDecimal previousWeekRevenue = sumOrderRevenue(previousWeekOrders);
        BigDecimal previousMonthRevenue = sumOrderRevenue(previousMonthOrders);

        // Calculate order counts
        long totalOrders = allOrders.size();
        long todayOrderCount = todayOrders.size();
        long thisWeekOrderCount = thisWeekOrders.size();
        long thisMonthOrderCount = thisMonthOrders.size();

        // Calculate growth rates
        double revenueGrowth = calculateGrowthRate(thisMonthRevenue, previousMonthRevenue);
        double orderGrowth = calculateGrowthRate(thisMonthOrderCount, previousWeekOrders.size());

        // Get user metrics
        long totalCustomers = userRepository.count();

        // Order status breakdown
        Map<String, Long> orderStatusCounts = allOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getStatus().name(),
                        Collectors.counting()
                ));

        // Calculate recent metrics
        long pendingOrders = countOrdersByStatus(allOrders, OrderStatus.PENDING_PAYMENT);
        long processingOrders = countOrdersByStatus(allOrders, OrderStatus.PROCESSING);
        long shippingOrders = countOrdersByStatus(allOrders, OrderStatus.SHIPPING);

        // Average order value
        BigDecimal averageOrderValue = allOrders.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(new BigDecimal(allOrders.size()), 2, RoundingMode.HALF_UP);

        // Build response
        return DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .thisWeekRevenue(thisWeekRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                .revenueGrowth(revenueGrowth)
                .totalOrders(totalOrders)
                .todayOrders(todayOrderCount)
                .thisWeekOrders(thisWeekOrderCount)
                .thisMonthOrders(thisMonthOrderCount)
                .orderGrowth(orderGrowth)
                .totalCustomers(totalCustomers)
                .orderStatusCounts(orderStatusCounts)
                .averageOrderValue(averageOrderValue)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .build();
    }

    // Helper classes for building response objects

    private static class TopProductBuilder {
        private UUID productId;
        private String productName;
        private String productSlug;
        private String productImage;
        private String categoryName;
        private String regionName;
        private int quantity = 0;
        private BigDecimal revenue = BigDecimal.ZERO;

        public TopProductBuilder(UUID productId, String productName, String productSlug,
                                 String productImage, String categoryName, String regionName) {
            this.productId = productId;
            this.productName = productName;
            this.productSlug = productSlug;
            this.productImage = productImage;
            this.categoryName = categoryName;
            this.regionName = regionName;
        }

        public void addItem(int itemQuantity, BigDecimal itemRevenue) {
            this.quantity += itemQuantity;
            this.revenue = this.revenue.add(itemRevenue);
        }

        public TopProductResponse build() {
            return TopProductResponse.builder()
                    .productId(productId)
                    .productName(productName)
                    .productSlug(productSlug)
                    .productImage(productImage)
                    .quantity(quantity)
                    .revenue(revenue)
                    .categoryName(categoryName)
                    .regionName(regionName)
                    .build();
        }
    }

    private static class GeographicSalesBuilder {
        private String provinceCode;
        private String provinceName;
        private int orderCount = 0;
        private BigDecimal revenue = BigDecimal.ZERO;

        public GeographicSalesBuilder(String provinceCode, String provinceName) {
            this.provinceCode = provinceCode;
            this.provinceName = provinceName;
        }

        public void addOrder(BigDecimal orderAmount) {
            this.orderCount++;
            this.revenue = this.revenue.add(orderAmount);
        }

        public GeographicSalesResponse build(long totalOrders, BigDecimal totalRevenue) {
            double percentageOfTotalOrders = totalOrders > 0
                    ? (double) orderCount / totalOrders * 100
                    : 0;

            double percentageOfTotalRevenue = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? revenue.divide(totalRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100
                    : 0;

            return GeographicSalesResponse.builder()
                    .provinceCode(provinceCode)
                    .provinceName(provinceName)
                    .orderCount(orderCount)
                    .revenue(revenue)
                    .percentageOfTotalOrders(percentageOfTotalOrders)
                    .percentageOfTotalRevenue(percentageOfTotalRevenue)
                    .build();
        }
    }

    private static class PaymentMethodBreakdownBuilder {
        private PaymentMethod paymentMethod;
        private String displayName;
        private int orderCount = 0;
        private BigDecimal revenue = BigDecimal.ZERO;

        public PaymentMethodBreakdownBuilder(PaymentMethod paymentMethod, String displayName) {
            this.paymentMethod = paymentMethod;
            this.displayName = displayName;
        }

        public void addOrder(BigDecimal orderAmount) {
            this.orderCount++;
            this.revenue = this.revenue.add(orderAmount);
        }

        public PaymentMethodBreakdownResponse build(long totalOrders, BigDecimal totalRevenue) {
            double percentageOfTotalOrders = totalOrders > 0
                    ? (double) orderCount / totalOrders * 100
                    : 0;

            double percentageOfTotalRevenue = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? revenue.divide(totalRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100
                    : 0;

            return PaymentMethodBreakdownResponse.builder()
                    .paymentMethod(paymentMethod)
                    .displayName(displayName)
                    .orderCount(orderCount)
                    .revenue(revenue)
                    .percentageOfTotalOrders(percentageOfTotalOrders)
                    .percentageOfTotalRevenue(percentageOfTotalRevenue)
                    .build();
        }
    }

    // Helper methods

    private <T> List<TimeSeriesDataPoint> generateTimeSeriesData(
            List<T> items,
            String period,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Function<T, BigDecimal> valueExtractor) {

        // Group data points by period
        Map<String, List<T>> groupedItems = groupByPeriod(items, period, startDate, endDate);

        // Create a list of all expected time points
        List<String> allTimePoints = generateTimePointLabels(period, startDate, endDate);

        // Create data points
        return allTimePoints.stream()
                .map(timePoint -> {
                    List<T> periodItems = groupedItems.getOrDefault(timePoint, Collections.emptyList());
                    BigDecimal value = periodItems.stream()
                            .map(valueExtractor)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TimeSeriesDataPoint.builder()
                            .label(timePoint)
                            .value(value)
                            .count(periodItems.size())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private <T> List<TimeSeriesDataPoint> generateAverageOrderValueSeries(
            List<T> items,
            String period,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        // Group data points by period
        Map<String, List<Order>> groupedItems = groupByPeriod(
                items.stream()
                        .map(item -> (Order) item)
                        .collect(Collectors.toList()),
                period,
                startDate,
                endDate
        );

        // Create a list of all expected time points
        List<String> allTimePoints = generateTimePointLabels(period, startDate, endDate);

        // Create data points with average values
        return allTimePoints.stream()
                .map(timePoint -> {
                    List<Order> periodOrders = groupedItems.getOrDefault(timePoint, Collections.emptyList());
                    BigDecimal totalValue = periodOrders.stream()
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal averageValue = periodOrders.isEmpty() ? BigDecimal.ZERO :
                            totalValue.divide(new BigDecimal(periodOrders.size()), 2, RoundingMode.HALF_UP);

                    return TimeSeriesDataPoint.builder()
                            .label(timePoint)
                            .value(averageValue)
                            .count(periodOrders.size())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private <T> Map<String, List<T>> groupByPeriod(
            List<T> items,
            String period,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (items.isEmpty()) {
            return Collections.emptyMap();
        }

        // Extract creation dates from items
        Map<String, List<T>> result = new HashMap<>();

        // Format date based on period type
        DateTimeFormatter formatter;
        Function<LocalDateTime, String> keyExtractor;

        switch (period.toLowerCase()) {
            case "daily":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                keyExtractor = date -> date.format(formatter);
                break;
            case "weekly":
                keyExtractor = date -> {
                    LocalDate ld = date.toLocalDate();
                    LocalDate weekStart = ld.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    return "Week " + weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                };
                break;
            case "monthly":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                keyExtractor = date -> date.format(formatter);
                break;
            case "yearly":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                keyExtractor = date -> date.format(formatter);
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                keyExtractor = date -> date.format(formatter);
                break;
        }

        // Group items by period
        return items.stream()
                .filter(item -> {
                    if (item instanceof Order) {
                        LocalDateTime orderDate = ((Order) item).getCreatedAt();
                        return orderDate != null &&
                                !orderDate.isBefore(startDate) &&
                                !orderDate.isAfter(endDate);
                    }
                    return true;
                })
                .collect(Collectors.groupingBy(item -> {
                    if (item instanceof Order) {
                        LocalDateTime orderDate = ((Order) item).getCreatedAt();
                        return keyExtractor.apply(orderDate);
                    }
                    return "unknown";
                }));
    }

    private List<String> generateTimePointLabels(String period, LocalDateTime startDate, LocalDateTime endDate) {
        List<String> labels = new ArrayList<>();
        DateTimeFormatter formatter;

        switch (period.toLowerCase()) {
            case "daily":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate current = startDate.toLocalDate();
                LocalDate end = endDate.toLocalDate();
                while (!current.isAfter(end)) {
                    labels.add(current.format(formatter));
                    current = current.plusDays(1);
                }
                break;

            case "weekly":
                LocalDate weekStart = startDate.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEnd = endDate.toLocalDate();
                while (!weekStart.isAfter(weekEnd)) {
                    labels.add("Week " + weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    weekStart = weekStart.plusWeeks(1);
                }
                break;

            case "monthly":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                YearMonth currentMonth = YearMonth.from(startDate);
                YearMonth endMonth = YearMonth.from(endDate);
                while (!currentMonth.isAfter(endMonth)) {
                    labels.add(currentMonth.format(formatter));
                    currentMonth = currentMonth.plusMonths(1);
                }
                break;

            case "yearly":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                int startYear = startDate.getYear();
                int endYear = endDate.getYear();
                for (int year = startYear; year <= endYear; year++) {
                    labels.add(String.valueOf(year));
                }
                break;

            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate defaultCurrent = startDate.toLocalDate();
                LocalDate defaultEnd = endDate.toLocalDate();
                while (!defaultCurrent.isAfter(defaultEnd)) {
                    labels.add(defaultCurrent.format(formatter));
                    defaultCurrent = defaultCurrent.plusDays(1);
                }
                break;
        }

        return labels;
    }

    private double calculateGrowthPercentage(List<TimeSeriesDataPoint> timeSeriesData) {
        if (timeSeriesData.size() <= 1) {
            return 0;
        }

        // Split data into two halves to compare
        int midpoint = timeSeriesData.size() / 2;

        // Calculate sum of values in first half
        BigDecimal firstHalfSum = timeSeriesData.subList(0, midpoint).stream()
                .map(TimeSeriesDataPoint::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate sum of values in second half
        BigDecimal secondHalfSum = timeSeriesData.subList(midpoint, timeSeriesData.size()).stream()
                .map(TimeSeriesDataPoint::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate growth percentage
        if (firstHalfSum.compareTo(BigDecimal.ZERO) == 0) {
            return secondHalfSum.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }

        return secondHalfSum.subtract(firstHalfSum)
                .divide(firstHalfSum, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private String getPaymentMethodDisplayName(PaymentMethod method) {
        switch (method) {
            case BANK_TRANSFER: return "Bank Transfer";
            case COD: return "Cash on Delivery";
            case VNPAY: return "VNPay";
            case MOMO: return "MoMo";
            default: return method.name();
        }
    }

    private List<Order> getOrdersInDateRange(List<Order> orders, LocalDateTime start, LocalDateTime end) {
        return orders.stream()
                .filter(order -> {
                    LocalDateTime orderDate = order.getCreatedAt();
                    return orderDate != null &&
                            !orderDate.isBefore(start) &&
                            !orderDate.isAfter(end);
                })
                .collect(Collectors.toList());
    }

    private BigDecimal sumOrderRevenue(List<Order> orders) {
        return orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countOrdersByStatus(List<Order> orders, OrderStatus status) {
        return orders.stream()
                .filter(order -> order.getStatus() == status)
                .count();
    }

    private double calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }

        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private double calculateGrowthRate(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100 : 0;
        }

        return ((double) (current - previous) / previous) * 100;
    }
}