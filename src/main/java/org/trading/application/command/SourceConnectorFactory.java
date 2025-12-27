package org.trading.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.trading.application.service.SourceConnector;
import org.trading.application.service.connector.BinanceSourceConnector;
import org.trading.application.service.connector.HoubiSourceConnector;
import org.trading.domain.enumeration.AggregatedSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SourceConnectorFactory {

    private final Map<AggregatedSource, SourceConnector> connectorCache = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;
    /**
     * Creates or retrieves a cached connector for the given provider.
     * @param source Provider source
     * @return Initialized connector instance
     */
    public SourceConnector getConnector(AggregatedSource source) {
        return connectorCache.computeIfAbsent(source, this::createConnector);
    }

    /**
     * Creates a new connector instance based on the provider source.
     */
    private SourceConnector createConnector(AggregatedSource source) {
        return switch (source) {
            case Binance -> applicationContext.getBean(BinanceSourceConnector.class);
            case Huobi -> applicationContext.getBean(HoubiSourceConnector.class);
        };
    }

    /**
     * Returns all active connectors.
     */
    public List<SourceConnector> getAllConnectors() {
        return new ArrayList<>(connectorCache.values());
    }

}
