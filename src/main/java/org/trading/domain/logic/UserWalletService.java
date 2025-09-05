package org.trading.domain.logic;

import java.math.BigDecimal;
import org.trading.insfrastructure.entities.UserWallet;

public interface UserWalletService {

  UserWallet deduct(UserWallet userId, BigDecimal amount);

  UserWallet add(UserWallet userId, BigDecimal amount);

  UserWallet get(String username);
}
