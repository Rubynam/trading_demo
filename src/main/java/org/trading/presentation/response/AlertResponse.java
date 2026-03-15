package org.trading.presentation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.trading.domain.enumeration.AlertCondition;
import org.trading.domain.enumeration.AlertStatus;
import org.trading.domain.enumeration.FrequencyCondition;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private String alertId;
    private String symbol;
    private String source;
    private BigDecimal targetPrice;
    private AlertCondition condition;
    private AlertStatus status;
    private FrequencyCondition frequencyCondition;
    private int hitCount;
    private int maxHits;  // Default: 10
    private Instant createdAt;
    private Instant updatedAt;
}
