package org.trading.application.port;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.trading.domain.aggregates.TickerData;
import org.trading.insfrastructure.mapper.BinanceTicker;

import static org.trading.util.NumberUtils.parseBigDecimal;

@Slf4j
@Component
public class TickerBinanceTransformer implements Transformer<BinanceTicker, TickerData> {

    @Nullable
    @Override
    public TickerData transform(BinanceTicker input) {
        if (input == null) {
            log.warn("Input BinanceTicker is null");
            return null;
        }

        try {
            return TickerData.builder()
                    .symbol(input.symbol())
                    .priceChange(parseBigDecimal(input.priceChange()))
                    .priceChangePercent(parseBigDecimal(input.priceChangePercent()))
                    .weightedAvgPrice(parseBigDecimal(input.weightedAvgPrice()))
                    .prevClosePrice(parseBigDecimal(input.prevClosePrice()))
                    .lastPrice(parseBigDecimal(input.lastPrice()))
                    .lastQty(parseBigDecimal(input.lastQty()))
                    .bidPrice(parseBigDecimal(input.bidPrice()))
                    .bidQty(parseBigDecimal(input.bidQty()))
                    .askPrice(parseBigDecimal(input.askPrice()))
                    .askQty(parseBigDecimal(input.askQty()))
                    .openPrice(parseBigDecimal(input.openPrice()))
                    .highPrice(parseBigDecimal(input.highPrice()))
                    .lowPrice(parseBigDecimal(input.lowPrice()))
                    .volume(parseBigDecimal(input.volume()))
                    .quoteVolume(parseBigDecimal(input.quoteVolume()))
                    .openTime(input.openTime())
                    .closeTime(input.closeTime())
                    .firstId(input.firstId())
                    .lastId(input.lastId())
                    .count(input.count())
                    .build();
        } catch (NumberFormatException e) {
            log.error("Failed to parse BinanceTicker to TickerData due to number format exception: {}", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            log.error("Failed to parse BinanceTicker to TickerData due to null pointer exception: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error while transforming BinanceTicker to TickerData: {}", e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public BinanceTicker reverseTransform(TickerData output) {
        // Reverse transformation not implemented
        return null;
    }
}