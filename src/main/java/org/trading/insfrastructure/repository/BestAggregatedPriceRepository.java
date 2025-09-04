package org.trading.insfrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.trading.insfrastructure.entities.BestAggregatedPrice;

@Repository
public interface BestAggregatedPriceRepository extends JpaRepository<BestAggregatedPrice, Long> {

}
