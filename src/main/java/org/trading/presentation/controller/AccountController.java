package org.trading.presentation.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trading.application.queries.AccountBalanceQueries;
import org.trading.presentation.response.AccountBalanceResponse;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

  private final AccountBalanceQueries queries;

  @GetMapping("/{username}")
  public AccountBalanceResponse getAccount(@PathVariable(value = "username") @Valid @NotBlank String username)
      throws Exception {
    return queries.getAccountBalanceBy(username);
  }
}
