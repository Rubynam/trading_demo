package org.trading.presentation.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trading.application.command.TradeCommand;
import org.trading.presentation.request.TradeRequest;
import org.trading.presentation.response.TradeResponse;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
@Validated
public class TradeController {

  private final TradeCommand tradeCommand;

  @PostMapping
  public TradeResponse trade(@Valid @RequestBody TradeRequest tradeRequest) throws Exception {
    return tradeCommand.execute(tradeRequest);
  }
}
