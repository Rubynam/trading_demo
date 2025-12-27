package org.trading.application.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceEventMessage implements Serializable {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Source is required")
    private String source;

    @NotNull(message = "Bid price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Bid price must be positive")
    private BigDecimal bidPrice;

    @NotNull(message = "Ask price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Ask price must be positive")
    private BigDecimal askPrice;

    @NotNull(message = "Timestamp is required")
    @PastOrPresent(message = "Timestamp cannot be in the future")
    private Long timestamp;

    @NotNull(message = "askQty price is required")
    private BigDecimal askQty;

    @NotNull(message = "bidQty price is required")
    private BigDecimal bidQty;

    @NotBlank(message = "Event ID is required")
    private String eventId;
}