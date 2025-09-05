package org.trading.application.command;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.trading.constant.TransactionStatus;
import org.trading.domain.logic.PriceService;
import org.trading.domain.logic.SymbolValidation;
import org.trading.domain.logic.TransactionService;
import org.trading.domain.logic.UserWalletService;
import org.trading.domain.logic.impl.UserBalanceValidation;
import org.trading.insfrastructure.entities.UserWallet;
import org.trading.presentation.request.TradeRequest;
import org.trading.presentation.response.BestPriceResponse;
import org.trading.presentation.response.TradeResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeCommand implements Command<TradeRequest, TradeResponse> {

  private final PriceService priceService;
  private final UserWalletService userWalletService;
  private final TransactionService transactionService;
  private final UserBalanceValidation userBalanceValidation;
  private final SymbolValidation symbolValidation;

  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Override
  public TradeResponse execute(TradeRequest input) throws Exception {
    var bestPrice = priceService.getBestPrice(input.getSymbol());
    if(bestPrice == null) throw new IllegalArgumentException("Invalid symbol");
    if(!symbolValidation.validate(input.getSymbol())) throw new IllegalArgumentException("Symbol does not allow");

    BigDecimal bidPrice = bestPrice.getBidPrice();
    BigDecimal askPrice = bestPrice.getAskPrice();
    BigDecimal price = getPrice(input, bidPrice, askPrice);

    log.info("Executing trade username={} side={} price={}",input.getUsername(),input.getSide(),price);

    UserWallet userWallet = userWalletService.get(input.getUsername());
    if (userWallet == null) throw new IllegalArgumentException("Invalid wallet");

    BigDecimal amount = price.multiply(BigDecimal.valueOf(input.getQuantity()));

    if(!userBalanceValidation.validate(userWallet,amount)){
      log.warn("Insufficient balance {}",userWallet);
      transactionService.store(userWallet.getUsername(),input.getSymbol(),price,BigDecimal.valueOf(input.getQuantity()),input.getSide(), TransactionStatus.FAILURE);
      return response(TransactionStatus.FAILURE,price, input);
    }

    userWallet = executeBalance(input, userWallet, amount);

    log.info("Trade executed username={} side={} babalance {}",userWallet,input.getSide(),userWallet.getBalance());
    log.info("write transaction logs");
    transactionService.store(userWallet.getUsername(),input.getSymbol(),price,BigDecimal.valueOf(input.getQuantity()),input.getSide(), TransactionStatus.SUCCESS);

    return response(TransactionStatus.SUCCESS, price, input);
  }

  private UserWallet executeBalance(TradeRequest input, UserWallet userWallet, BigDecimal amount) {
    return switch (input.getSide()) {
      case BUY -> userWalletService.deduct(userWallet, amount);
      case SELL -> userWalletService.add(userWallet, amount);
    };
  }

  private static BigDecimal getPrice(TradeRequest input, BigDecimal bidPrice, BigDecimal askPrice) {
    return switch (input.getSide()) {
      case BUY -> bidPrice;
      case SELL -> askPrice;
    };
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
