package com.mosiacstore.mosiac.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private UUID id;
    private String guestId;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private LocalDateTime expiredAt;
    private LocalDateTime updatedAt;
    private List<CartItemResponse> items;
}
