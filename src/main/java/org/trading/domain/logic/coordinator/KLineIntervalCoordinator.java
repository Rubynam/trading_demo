package org.trading.domain.logic.coordinator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.trading.domain.aggregates.KLineData;
import org.trading.domain.enumeration.KLineInterval;
import org.trading.domain.logic.impl.BinanceKLineService;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class KLineIntervalCoordinator {


    @Value("${source.binance.kline-white-list-symbol}")
    private Set<String> symbols;
    private final BinanceKLineService binanceKLineService;


    public List<KLineData> setupCrawInternal(){
        KLineInterval[] kLineInterval =KLineInterval.values();
        for(int i=0;i<kLineInterval.length;i++){
            symbols.stream().forEach(symbol->{

            });
        }
    }
}
