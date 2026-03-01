package org.trading.application.port;

import org.springframework.stereotype.Component;
import org.trading.insfrastructure.entities.PriceAlertEntity;
import org.trading.presentation.request.AlertRequest;

@Component
public class PriceAlertTransformer implements Transformer<AlertRequest, PriceAlertEntity> {
    @Override
    public PriceAlertEntity transform(AlertRequest input) throws IllegalArgumentException {
        return PriceAlertEntity.builder()
                .targetPrice(input.getTargetPrice())
                .symbol(input.getSymbol())
                .source(input.getSource().name())
                .condition(input.getCondition().name())
                .hitCount(input.getHitCount())
                .build();
    }

    @Override
    public AlertRequest reverseTransform(PriceAlertEntity output) throws IllegalArgumentException {
        throw new UnsupportedOperationException("this method is unsupported");
    }
}
