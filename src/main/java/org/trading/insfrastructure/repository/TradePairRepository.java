package org.trading.insfrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.trading.insfrastructure.entities.TradePair;

@Repository
public interface TradePairRepository extends JpaRepository<TradePair,Long> {

}
