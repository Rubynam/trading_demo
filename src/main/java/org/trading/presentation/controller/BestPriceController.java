package org.trading.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trading.application.queries.PriceQueries;
import org.trading.presentation.response.BestPriceResponse;

@RestController
@RequestMapping("/price")
@RequiredArgsConstructor
@Slf4j
public class BestPriceController {

  private final PriceQueries queries;


  @GetMapping("/best/{symbol}")
  public BestPriceResponse getBestPrice(@PathVariable(value = "symbol") String symbol) {
    return queries.getBestPriceBy(symbol);
  }
}
