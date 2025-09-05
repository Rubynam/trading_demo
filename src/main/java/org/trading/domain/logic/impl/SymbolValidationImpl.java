package org.trading.domain.logic.impl;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.trading.domain.logic.SymbolValidation;
import org.trading.insfrastructure.repository.TradePairRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class SymbolValidationImpl implements SymbolValidation {

  private static final Set<String> SYMBOL_AVAILABLE = new CopyOnWriteArraySet<>();

  private final TradePairRepository tradePairRepository;;

  @PostConstruct
  void setup(){
    tradePairRepository.findAll().forEach(item->{
      String symbol = String.format("%s%s", item.getBaseCurrency(), item.getQuoteCurrency());
      SYMBOL_AVAILABLE.add(symbol);
    });
  }

  @Override
  public boolean validate(String symbol) {
    return SYMBOL_AVAILABLE.contains(symbol);
  }
}
