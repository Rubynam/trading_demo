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
  public UserWallet deduct(UserWallet wallet, BigDecimal amount) {
    log.info("Deducting {} from wallet {}", amount, wallet);
    if(wallet.getBalance().compareTo(amount) >= 0) {
      wallet.setBalance(wallet.getBalance().subtract(amount));
      return userWalletRepository.save(wallet);
    }
    throw new IllegalArgumentException("Insufficient funds");
  }

  @Override
  public UserWallet add(UserWallet username, BigDecimal amount) {
    log.info("Adding {} to wallet {}", amount, username);
    username.setBalance(username.getBalance().add(amount));
    return userWalletRepository.save(username);
  }

  @Override
  public UserWallet get(String username) {
    return userWalletRepository.findUserWalletByUsername(username).orElse(null);
  }
}
