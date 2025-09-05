package org.trading.domain.logic;

import java.math.BigDecimal;
import org.trading.insfrastructure.enumeration.TradeSide;

public interface TransactionService {

  boolean store(String username, String symbol, BigDecimal price, int quantity, TradeSide side) throws Exception;
}
