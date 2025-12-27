package org.trading.application.port;

import java.math.BigDecimal;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.trading.domain.aggregates.AggregationPrice;
import org.trading.insfrastructure.mapper.BinanceData;

@Service
public class BinanceDataTransformer implements Transformer<BinanceData, AggregationPrice> {

  private static final String VALID_DIGIT = "^[0-9]+(\\.[0-9]+)?$";

  @Override
  public AggregationPrice transform(BinanceData input) throws IllegalArgumentException {
    if(Objects.isNull(input) || StringUtils.isBlank(input.getSymbol()) || Objects.isNull(input.getBidPrice()) || Objects.isNull(input.getAskPrice()))
      throw new IllegalArgumentException("Invalid input");

    if(input.getBidPrice().isEmpty() || input.getAskPrice().isEmpty())
      throw new IllegalArgumentException("Invalid input");

    if (!input.getBidPrice().matches(VALID_DIGIT) || !input.getAskPrice().matches(VALID_DIGIT))
      throw new IllegalArgumentException("Invalid input");

    if(Double.parseDouble(input.getBidPrice()) < 0 || Double.parseDouble(input.getAskPrice()) < 0)
      throw new IllegalArgumentException("Invalid input");


    return AggregationPrice.builder()
        .symbol(input.getSymbol())
        .bidPrice(BigDecimal.valueOf(Double.parseDouble(input.getBidPrice())))
        .askPrice(BigDecimal.valueOf(Double.parseDouble(input.getAskPrice())))
        .bidQty(BigDecimal.valueOf(Double.parseDouble(input.getBidQty())))
        .askQty(BigDecimal.valueOf(Double.parseDouble(input.getAskPrice())))
        .build();
  }

  @Override
  public BinanceData reverseTransform(AggregationPrice output) throws IllegalArgumentException {
    //todo
    return null;
  }
}
