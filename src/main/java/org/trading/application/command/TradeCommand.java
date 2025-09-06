package org.trading.application.command;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.trading.common.PairWallet;
import org.trading.constant.TransactionStatus;
import org.trading.domain.logic.PriceService;
import org.trading.domain.logic.SymbolValidation;
import org.trading.domain.logic.TransactionService;
import org.trading.domain.logic.UserWalletService;
import org.trading.domain.logic.impl.UserBalanceValidation;
import org.trading.insfrastructure.enumeration.TradeSide;
import org.trading.presentation.request.TradeRequest;
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

    boolean isSuccess = executeBalance(pairWallet, input, price);

    if(!isSuccess) {
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

  boolean executeBalance(PairWallet pairWallet, TradeRequest input, BigDecimal price)
      throws Exception {
    BigDecimal amount = price.multiply(BigDecimal.valueOf(input.getQuantity()));
    final TradeSide side = input.getSide();

    if(!userBalanceValidation.validate(pairWallet,amount, input)){
      log.warn("Insufficient balance username={} amount {} action {} symbol {}",input.getUsername(),amount,input.getSide(),input.getSymbol());
      transactionService.store(input.getUsername(),input.getSymbol(),price,BigDecimal.valueOf(input.getQuantity()),input.getSide(), TransactionStatus.FAILURE);
      return false;
    }
    switch (side) {
      case BUY: {
        userWalletService.deduct(pairWallet.getQuoteWallet(), amount);
        userWalletService.add(pairWallet.getBaseWallet(), BigDecimal.valueOf(input.getQuantity()));
        break;
      }
      case SELL: {
        userWalletService.deduct(pairWallet.getBaseWallet(), BigDecimal.valueOf(input.getQuantity()));
        userWalletService.add(pairWallet.getQuoteWallet(), amount);
        break;
      }
      default:
        log.warn("Invalid side {}",side);
        return false;
    }

    return true;
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
