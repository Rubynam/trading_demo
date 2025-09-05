package org.trading.application.queries;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.trading.domain.logic.UserWalletService;
import org.trading.presentation.response.AccountBalanceResponse;

@Service
@RequiredArgsConstructor
public class AccountBalanceQueries {

  private final UserWalletService userWalletService;


  public AccountBalanceResponse getAccountBalanceBy(String username) throws Exception {
    var wallet = userWalletService.get(username);
    if(wallet == null) throw new IllegalArgumentException("Invalid username");
    return AccountBalanceResponse.builder()
        .username(wallet.getUsername())
        .balance(wallet.getBalance())
        .build();
  }
}
