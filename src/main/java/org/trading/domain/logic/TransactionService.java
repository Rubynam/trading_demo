package org.trading.domain.logic;

import java.math.BigDecimal;
import org.trading.constant.TransactionStatus;
import org.trading.insfrastructure.enumeration.TradeSide;

public interface TransactionService {

  void store(String username, String symbol, BigDecimal price, BigDecimal quantity, TradeSide side, TransactionStatus status) throws Exception;
}
