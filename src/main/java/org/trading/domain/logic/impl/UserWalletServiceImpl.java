package org.trading.domain.logic.impl;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.trading.domain.logic.UserWalletService;
import org.trading.insfrastructure.entities.UserWallet;
import org.trading.insfrastructure.repository.UserWalletRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserWalletServiceImpl implements UserWalletService {

  private final UserWalletRepository userWalletRepository;

  @Override
  public UserWallet deduct(String username, BigDecimal amount) {
    log.info("Deducting {} from user wallet {}", amount, username);
    return null;
  }

  @Override
  public UserWallet add(String username, BigDecimal amount) {
    return null;
  }

  @Override
  public UserWallet get(String username) {
    return userWalletRepository.findUserWalletByUsername(username).orElse(null);
  }
}
