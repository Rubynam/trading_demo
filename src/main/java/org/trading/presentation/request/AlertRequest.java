package org.trading.presentation.request;

import lombok.Data;
import org.trading.domain.enumeration.AggregatedSource;
import org.trading.domain.enumeration.AlertCondition;

import java.math.BigDecimal;

@Data
public class AlertRequest {
    private String symbol;
    private AggregatedSource source;
    private BigDecimal targetPrice;
    private AlertCondition condition;
    private int hitCount;
}
