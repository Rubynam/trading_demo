package org.trading.application.queries;


import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.trading.domain.logic.UserWalletService;
import org.trading.presentation.response.AccountBalanceResponse;

@Service
@RequiredArgsConstructor
public class AccountBalanceQueries {

  private final UserWalletService userWalletService;


  public List<AccountBalanceResponse> getAccountBalanceBy(String username) throws Exception {
    var wallets = userWalletService.get(username);

    if (wallets == null || wallets.isEmpty()) {
      throw new IllegalArgumentException("Invalid username");
    }

    return wallets.stream().map(wallet -> AccountBalanceResponse.builder()
        .username(wallet.getUsername())
        .balance(wallet.getBalance())
        .currency(wallet.getCurrency())
        .build()).toList();
  }
}
