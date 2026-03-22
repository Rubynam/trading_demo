package org.trading.domain.aggregates;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 {
 "time": "2026-03-21 15:00:00",
 "open": 64000.00,
 "high": 64500.00,
 "low": 63800.00,
 "close": 64200.00,
 "volume": 1250.5
 },
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KLineData {

   private String time;
   private BigDecimal open;
   private BigDecimal high;
   private BigDecimal low;
   private BigDecimal close;
   private BigDecimal volume;
   private String symbol;

}
