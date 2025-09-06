package org.trading.domain.logic;

import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.trading.common.PairWallet;
import org.trading.insfrastructure.entities.UserWallet;

public interface UserWalletService {

  UserWallet deduct(UserWallet userId, BigDecimal amount);

  UserWallet add(UserWallet userId, BigDecimal amount);

  PairWallet get(String username, String baseCurrency, String quoteCurrency) throws Exception;

  List<UserWallet> get(String username);

  Pair<String,String> extractCurrency(String symbol);
}
