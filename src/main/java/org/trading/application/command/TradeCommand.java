package org.trading.application.command;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.trading.domain.aggregates.PairWallet;
import org.trading.domain.enumeration.TransactionStatus;
import org.trading.domain.logic.PriceService;
import org.trading.domain.logic.SymbolValidation;
import org.trading.domain.logic.TransactionService;
import org.trading.domain.logic.UserWalletService;
import org.trading.domain.logic.impl.TransactionExecutionService;
import org.trading.presentation.request.TradeRequest;
import org.trading.presentation.response.TradeResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeCommand implements Command<TradeRequest, TradeResponse> {

  private final PriceService priceService;
  private final UserWalletService userWalletService;
  private final TransactionService transactionService;
  private final SymbolValidation symbolValidation;
  private final TransactionExecutionService transactionExecutionService;

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Override
  public TradeResponse execute(TradeRequest input) throws Exception {
    var bestPrice = priceService.getBestPrice(input.getSymbol());
    if(bestPrice == null) throw new IllegalArgumentException("Invalid symbol");
    if(!symbolValidation.validate(input.getSymbol())) throw new IllegalArgumentException("Symbol does not allow");

    BigDecimal bidPrice = bestPrice.getBidPrice();
    BigDecimal askPrice = bestPrice.getAskPrice();
    BigDecimal price = getPrice(input, bidPrice, askPrice);

    log.info("Executing trade username={} side={} price={}",input.getUsername(),input.getSide(),price);
    Pair<String,String> currency = userWalletService.extractCurrency(input.getSymbol());

    PairWallet pairWallet = userWalletService.get(input.getUsername(),currency.getLeft(), currency.getRight());

    boolean isSuccess = executeBalanceInTransaction(pairWallet, input, price);

    if(!isSuccess) {
      transactionService.store(input.getUsername(),input.getSymbol(),price,BigDecimal.valueOf(input.getQuantity()),input.getSide(), TransactionStatus.FAILURE);
      return response(TransactionStatus.FAILURE,price, input);
    }

    log.info("write transaction logs");
    transactionService.store(input.getUsername(),input.getSymbol(),price,BigDecimal.valueOf(input.getQuantity()),input.getSide(), TransactionStatus.SUCCESS);

    return response(TransactionStatus.SUCCESS, price, input);
  }

  private static BigDecimal getPrice(TradeRequest input, BigDecimal bidPrice, BigDecimal askPrice) {
    return switch (input.getSide()) {
      case BUY -> bidPrice;
      case SELL -> askPrice;
    };
  }

  boolean executeBalanceInTransaction(PairWallet pairWallet, TradeRequest input, BigDecimal price){
    try{
      //prevent Transactional self-invocation, which does not lead to an actual transaction at runtime
      return transactionExecutionService.executeBalance(pairWallet, input, price);
    }catch (Exception e){
      log.error("Error in transaction",e);
      return false;
    }
  }

  private static TradeResponse response(TransactionStatus status, BigDecimal price,
      TradeRequest input) {
    return TradeResponse.builder()
        .status(status)
        .price(price.toString())
        .side(input.getSide().toString())
        .quantity(String.valueOf(input.getQuantity()))
        .symbol(input.getSymbol())
        .build();
  }
}
