package org.trading.application.port;

import org.springframework.stereotype.Component;
import org.trading.domain.enumeration.AlertCondition;
import org.trading.domain.enumeration.AlertStatus;
import org.trading.insfrastructure.entities.PriceAlertEntity;
import org.trading.presentation.response.AlertResponse;

@Component
public class PriceAlertResponseTransformer implements Transformer<PriceAlertEntity, AlertResponse> {
    @Override
    public AlertResponse transform(PriceAlertEntity input) throws IllegalArgumentException {

        return AlertResponse.builder()
                .alertId(input.getAlertId())
                .source(input.getSource())
                .symbol(input.getSymbol())
                .targetPrice(input.getTargetPrice())
                .condition(AlertCondition.valueOf(input.getCondition()))
                .status(AlertStatus.valueOf(input.getStatus()))
                .maxHits(input.getMaxHits())
                .hitCount(input.getHitCount())
                .createdAt(input.getCreatedAt())
                .updatedAt(input.getUpdatedAt())
                .build();
    }

    @Override
    public PriceAlertEntity reverseTransform(AlertResponse output) throws IllegalArgumentException {
        return null;
    }
}
