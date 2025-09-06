package org.trading.insfrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.trading.insfrastructure.entities.TradeTransaction;

@Repository
public interface TradeTransactionRepository extends JpaRepository<TradeTransaction, Long> {

  Page<TradeTransaction> findAllByUsernameOrderByTimestampDesc(String username, Pageable pageable);
}
