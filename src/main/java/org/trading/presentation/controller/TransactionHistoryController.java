package org.trading.presentation.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.trading.application.queries.TransactionHistoryQueries;
import org.trading.presentation.response.TransactionHistoryResponse;

@RestController
@RequestMapping("/transaction/history")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TransactionHistoryController {

  private final TransactionHistoryQueries queries;


  @GetMapping("/{username}")
  public Page<TransactionHistoryResponse> getTransactionHistory(
      @PathVariable(value = "username") String username,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size) {
    return queries.getTransactionHistoryBy(username, page, size);
  }
}
