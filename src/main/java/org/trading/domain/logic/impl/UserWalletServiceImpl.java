package org.trading.domain.logic.impl;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.trading.domain.aggregates.PairWallet;
import org.trading.domain.logic.UserWalletService;
import org.trading.insfrastructure.entities.UserWallet;
import org.trading.insfrastructure.repository.UserWalletRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserWalletServiceImpl implements UserWalletService {

  public static final String STABLE_COIN_USDT = "USDT";
  private final UserWalletRepository userWalletRepository;

  @Override
  public UserWallet deduct(UserWallet wallet, BigDecimal amount) {
    log.info("Deducting {} from wallet {}", amount, wallet);
    if (wallet.getBalance().compareTo(amount) >= 0) {
      wallet.setBalance(wallet.getBalance().subtract(amount));
      return userWalletRepository.save(wallet);
    }
    throw new IllegalArgumentException("Insufficient funds");
  }

  @Override
  public UserWallet add(UserWallet userWallet, BigDecimal amount) {
    log.info("Adding {} to wallet {}", amount, userWallet);
    userWallet.setBalance(userWallet.getBalance().add(amount));
    return userWalletRepository.save(userWallet);
  }

  /**
   * @return a pair wallet. Left indicate baseCurrency (USDT) Right indicate quoteCurrency (BTC)
   */
  @Override
  public PairWallet get(String username, String baseCurrency, String quoteCurrency)
      throws Exception {
    if (!userWalletRepository.existsUserWalletByUsername(username)) {
      throw new IllegalArgumentException("Invalid username");
    }

    UserWallet userWallet = userWalletRepository.findUserWalletByUsernameAndCurrency(username,
        baseCurrency).orElse(buildUserWaller(username, baseCurrency));
    UserWallet quoteWallet = userWalletRepository.findUserWalletByUsernameAndCurrency(username,
        quoteCurrency).orElse(buildUserWaller(username, quoteCurrency));

    return new PairWallet(userWallet, quoteWallet);
  }

  @Override
  public List<UserWallet> get(String username) {
    return userWalletRepository.findUserWalletByUsername(username);
  }

  @Override
  public Pair<String, String> extractCurrency(String symbol) {
    if (!symbol.endsWith(STABLE_COIN_USDT)) {
      throw new IllegalArgumentException("Symbol must include USDT as quote currency");
    }
    String baseCurrency = symbol.replace(STABLE_COIN_USDT, "");
    if (baseCurrency.isEmpty()) {
      throw new IllegalArgumentException("Invalid symbol format");
    }
    return Pair.of(baseCurrency, STABLE_COIN_USDT);
  }

  private UserWallet buildUserWaller(String username, String currency) {
    return UserWallet.builder().username(username)
        .currency(currency)
        .balance(BigDecimal.ZERO)
        .build();
  }
}
