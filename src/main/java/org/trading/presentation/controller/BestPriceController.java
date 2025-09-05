package org.trading.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trading.domain.logic.PriceService;
import org.trading.presentation.response.BestPriceDto;

@RestController
@RequestMapping("/price")
@RequiredArgsConstructor
@Slf4j
public class BestPriceController {

  private final PriceService priceService;


  @GetMapping("/best/{symbol}")
  public BestPriceDto getBestPrice(@PathVariable(value = "symbol") String symbol) {
    return priceService.getBestPrice(symbol);
  }
}
