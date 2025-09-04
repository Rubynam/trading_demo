package org.trading.insfrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.trading.insfrastructure.entities.TradeTransaction;

public interface TradeTransactionRepository extends JpaRepository<TradeTransaction, Long> {

}
