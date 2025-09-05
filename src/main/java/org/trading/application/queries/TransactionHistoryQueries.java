package org.trading.application.queries;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.trading.domain.logic.TransactionService;
import org.trading.insfrastructure.entities.TradeTransaction;
import org.trading.presentation.response.TransactionHistoryResponse;

@Service
@RequiredArgsConstructor
public class TransactionHistoryQueries {

  private final TransactionService transactionService;

  public Page<TransactionHistoryResponse> getTransactionHistoryBy(String username, int page, int sizePerPage) {
    Pageable pageable = Pageable.ofSize(sizePerPage).withPage(page);
    return transactionService.findAllBy(username,pageable).map(this::convert);
  }

  private TransactionHistoryResponse convert(TradeTransaction tradeTransaction) {
    return TransactionHistoryResponse.builder()
        .username(tradeTransaction.getUsername())
        .symbol(tradeTransaction.getSymbol())
        .transactionId(tradeTransaction.getId())
        .side(tradeTransaction.getTradeType().name())
        .price(tradeTransaction.getPrice().toString())
        .quantity(tradeTransaction.getQuantity().toString())
        .status(tradeTransaction.getStatus())
        .timestamp(tradeTransaction.getTimestamp())
        .build();
  }

}
