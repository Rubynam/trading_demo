package org.trading.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.trading.application.port.PriceAlertResponseTransformer;
import org.trading.application.port.PriceAlertTransformer;
import org.trading.domain.logic.impl.AlertManagementService;
import org.trading.presentation.request.AlertRequest;
import org.trading.presentation.response.AlertResponse;

@Service
@RequiredArgsConstructor
public class AlertCommand implements Command<AlertRequest, AlertResponse> {

    private final AlertManagementService alertManagementService;
    private final PriceAlertTransformer priceAlertTransformer;
    private final PriceAlertResponseTransformer priceAlertResponseTransformer;

    @Override
    public AlertResponse execute(AlertRequest input) throws Exception {
        var entity = priceAlertTransformer.transform(input);
        var result = alertManagementService.post(entity);
        return priceAlertResponseTransformer.transform(result);
    }
}
