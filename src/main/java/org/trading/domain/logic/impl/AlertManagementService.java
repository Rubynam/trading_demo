package org.trading.domain.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.trading.domain.enumeration.AlertStatus;
import org.trading.insfrastructure.entities.PriceAlertEntity;
import org.trading.insfrastructure.repository.PriceAlertRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertManagementService {

    private final PriceAlertRepository priceAlertRepository;

    private static final int MAXIMUM_HIT_ALERT = 50;

    public PriceAlertEntity post(@NonNull PriceAlertEntity entity){
        if(entity.getHitCount() > MAXIMUM_HIT_ALERT){
            throw new IllegalArgumentException("given hitCounts > MAX");
        }
        enrichData(entity);
        return priceAlertRepository.save(entity);
    }

    void enrichData(PriceAlertEntity alertEntity){
        alertEntity.setMaxHits(MAXIMUM_HIT_ALERT);
        alertEntity.setStatus(AlertStatus.ENABLED.name());
        alertEntity.setHitCount(0);
    }

}
