package org.trading.application.port;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.trading.domain.aggregates.KLineData;
import org.trading.insfrastructure.mapper.BinanceKLine;

import static org.trading.util.NumberUtils.parseBigDecimal;

@Slf4j
@Component
public class KLineBinanceTransformer implements Transformer<BinanceKLine, KLineData> {

    @Nullable
    @Override
    public KLineData transform(BinanceKLine input) {
        if (input == null) {
            log.warn("Input BinanceKLine is null");
            return null;
        }

        try {
            return KLineData.builder()
                    .time(formatTime(input.openTime()))
                    .open(parseBigDecimal(input.open()))
                    .high(parseBigDecimal(input.high()))
                    .low(parseBigDecimal(input.low()))
                    .close(parseBigDecimal(input.close()))
                    .volume(parseBigDecimal(input.volume()))
                    .build();
        } catch (NumberFormatException e) {
            log.error("Failed to parse BinanceKLine to KLineData due to number format exception: {}", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            log.error("Failed to parse BinanceKLine to KLineData due to null pointer exception: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error while transforming BinanceKLine to KLineData: {}", e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public BinanceKLine reverseTransform(KLineData output) {
        // Reverse transformation not implemented
        return null;
    }

    /**
     * Format timestamp to string
     * TODO: need to implement proper date formatting
     */
    @Nullable
    private String formatTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        // For now, just convert to string
        // TODO: Format as "yyyy-MM-dd HH:mm:ss"
        return String.valueOf(timestamp);
    }
}
