package org.trading.application.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.trading.application.port.KLineBinanceTransformer;
import org.trading.application.port.TickerBinanceTransformer;
import org.trading.domain.aggregates.KLineData;
import org.trading.domain.aggregates.TickerData;
import org.trading.domain.enumeration.Interval;
import org.trading.domain.logic.impl.BinanceDepthService;
import org.trading.domain.logic.impl.BinanceKLineService;
import org.trading.domain.logic.impl.BinanceTickerService;
import org.trading.domain.model.KLineParameters;
import org.trading.insfrastructure.mapper.BinanceDepth;
import org.trading.insfrastructure.mapper.BinanceKLine;
import org.trading.insfrastructure.mapper.BinanceTicker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BinanceDataJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BinanceKLineService kLineService;
    private final BinanceTickerService tickerService;
    private final BinanceDepthService depthService;
    private final KLineBinanceTransformer kLineTransformer;
    private final TickerBinanceTransformer tickerTransformer;

    @Value("#{'${source.binance.kline-white-list-symbol}'.split(',')}")
    private List<String> symbols;

    private static final String BASE_DIR = "binance-data";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy");

    /**
     * Create directory structure with current date and interval
     * Format: binance-data/<DD_MM_YYYY>/<KLineInterval>
     */
    private Path createDirectoryStructure(Interval interval) throws IOException {
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        Path intervalPath = Paths.get(BASE_DIR, currentDate, interval.getInterval());
        Files.createDirectories(intervalPath);
        return intervalPath;
    }

    /**
     * KLine Job for a specific interval
     */
    public Job createKLineJob(Interval interval) {
        return new JobBuilder("klineJob_" + interval.getInterval(), jobRepository)
                .start(createKLineStep(interval))
                .build();
    }

    /**
     * Ticker Job for a specific interval
     */
    public Job createTickerJob(Interval interval) {
        return new JobBuilder("tickerJob_" + interval.getInterval(), jobRepository)
                .start(createTickerStep(interval))
                .build();
    }

    /**
     * Depth Job for a specific interval
     */
    public Job createDepthJob(Interval interval) {
        return new JobBuilder("depthJob_" + interval.getInterval(), jobRepository)
                .start(createDepthStep(interval))
                .build();
    }

    /**
     * Step for KLine data processing
     */
    private Step createKLineStep(Interval interval) {
        return new StepBuilder("klineStep_" + interval.getInterval(), jobRepository)
                .<KLineData, KLineData>chunk(10, transactionManager)
                .reader(createKLineReader(interval))
                .writer(createKLineCsvWriter(interval))
                .build();
    }

    /**
     * Reader for KLine data
     */
    private ListItemReader<KLineData> createKLineReader(Interval interval) {
        try {
            List<KLineData> allData = symbols.stream()
                    .flatMap(symbol -> {
                        try {
                            KLineParameters params = new KLineParameters(symbol, interval.getInterval(), 12);
                            List<BinanceKLine> rawData = kLineService.craw(params);
                            return rawData.stream()
                                    .map(kLineTransformer::transform)
                                    .filter(Objects::nonNull)
                                    .peek(data -> data.setSymbol(symbol));
                        } catch (Exception e) {
                            log.error("Error fetching KLine data for symbol {}: {}", symbol, e.getMessage());
                            return java.util.stream.Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());
            return new ListItemReader<>(allData);
        } catch (Exception e) {
            log.error("Error in KLine reader: {}", e.getMessage());
            return new ListItemReader<>(List.of());
        }
    }

    /**
     * Step for Ticker data processing
     */
    private Step createTickerStep(Interval interval) {
        return new StepBuilder("tickerStep_" + interval.getInterval(), jobRepository)
                .<TickerData, TickerData>chunk(10, transactionManager)
                .reader(createTickerReader(interval))
                .writer(createTickerCsvWriter(interval))
                .build();
    }

    /**
     * Reader for Ticker data
     */
    private ListItemReader<TickerData> createTickerReader(Interval interval) {
        try {
            List<TickerData> allData = symbols.stream()
                    .map(symbol -> {
                        try {
                            BinanceTicker rawData = tickerService.craw(symbol,interval.getTickerInternval());
                            return tickerTransformer.transform(rawData);
                        } catch (Exception e) {
                            log.error("Error fetching Ticker data for symbol {}: {}", symbol, e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return new ListItemReader<>(allData);
        } catch (Exception e) {
            log.error("Error in Ticker reader: {}", e.getMessage());
            return new ListItemReader<>(List.of());
        }
    }

    /**
     * Step for Depth data processing
     */
    private Step createDepthStep(Interval interval) {
        return new StepBuilder("depthStep_" + interval.getInterval(), jobRepository)
                .<BinanceDepth, BinanceDepth>chunk(10, transactionManager)
                .reader(createDepthReader(interval))
                .writer(createDepthCsvWriter(interval))
                .build();
    }

    /**
     * Reader for Depth data
     */
    private ListItemReader<BinanceDepth> createDepthReader(Interval interval) {
        try {
            List<BinanceDepth> allData = symbols.stream()
                    .map(symbol -> {
                        try {
                            return depthService.craw(symbol, 10);
                        } catch (Exception e) {
                            log.error("Error fetching Depth data for symbol {}: {}", symbol, e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return new ListItemReader<>(allData);
        } catch (Exception e) {
            log.error("Error in Depth reader: {}", e.getMessage());
            return new ListItemReader<>(List.of());
        }
    }

    /**
     * CSV Writer for KLine data
     */
    private ItemWriter<KLineData> createKLineCsvWriter(Interval interval) {
        return items -> {
            try {
                Path dirPath = createDirectoryStructure(interval);
                File csvFile = dirPath.resolve("kline.csv").toFile();
                boolean fileExists = csvFile.exists();

                try (FileWriter writer = new FileWriter(csvFile, true)) {
                    // Write header if file is new
                    if (!fileExists) {
                        writer.write("symbol,time,open,high,low,close,volume\n");
                    }

                    // Write data with null safety
                    for (KLineData data : items) {
                        if (data == null) continue;

                        writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                                safeString(data.getSymbol()),
                                safeString(data.getTime()),
                                safeValue(data.getOpen()),
                                safeValue(data.getHigh()),
                                safeValue(data.getLow()),
                                safeValue(data.getClose()),
                                safeValue(data.getVolume())
                        ));
                    }
                }
                log.info("Written {} KLine records to {}", items.size(), csvFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Error writing KLine CSV: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * CSV Writer for Ticker data
     */
    private ItemWriter<TickerData> createTickerCsvWriter(Interval interval) {
        return items -> {
            try {
                Path dirPath = createDirectoryStructure(interval);
                File csvFile = dirPath.resolve("ticker.csv").toFile();
                boolean fileExists = csvFile.exists();

                try (FileWriter writer = new FileWriter(csvFile, true)) {
                    // Write header if file is new
                    if (!fileExists) {
                        writer.write("symbol,priceChange,priceChangePercent,lastPrice,bidPrice,askPrice,volume,openTime,closeTime\n");
                    }

                    // Write data with null safety
                    for (TickerData data : items) {
                        if (data == null) continue;

                        writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                                safeString(data.getSymbol()),
                                safeValue(data.getPriceChange()),
                                safeValue(data.getPriceChangePercent()),
                                safeValue(data.getLastPrice()),
                                safeValue(data.getBidPrice()),
                                safeValue(data.getAskPrice()),
                                safeValue(data.getVolume()),
                                safeValue(data.getOpenTime()),
                                safeValue(data.getCloseTime())
                        ));
                    }
                }
                log.info("Written {} Ticker records to {}", items.size(), csvFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Error writing Ticker CSV: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * CSV Writer for Depth data
     */
    private ItemWriter<BinanceDepth> createDepthCsvWriter(Interval interval) {
        return items -> {
            try {
                Path dirPath = createDirectoryStructure(interval);
                File csvFile = dirPath.resolve("depth.csv").toFile();
                boolean fileExists = csvFile.exists();

                try (FileWriter writer = new FileWriter(csvFile, true)) {
                    // Write header if file is new
                    if (!fileExists) {
                        writer.write("lastUpdateId,topBidPrice,topBidQty,topAskPrice,topAskQty\n");
                    }

                    // Write data with null safety
                    for (BinanceDepth data : items) {
                        if (data == null) continue;

                        String topBidPrice = "";
                        String topBidQty = "";
                        String topAskPrice = "";
                        String topAskQty = "";

                        // Safe extraction of bid data
                        if (data.bids() != null && !data.bids().isEmpty()) {
                            List<String> topBid = data.bids().get(0);
                            if (topBid != null && topBid.size() >= 2) {
                                topBidPrice = safeString(topBid.get(0));
                                topBidQty = safeString(topBid.get(1));
                            }
                        }

                        // Safe extraction of ask data
                        if (data.asks() != null && !data.asks().isEmpty()) {
                            List<String> topAsk = data.asks().get(0);
                            if (topAsk != null && topAsk.size() >= 2) {
                                topAskPrice = safeString(topAsk.get(0));
                                topAskQty = safeString(topAsk.get(1));
                            }
                        }

                        writer.write(String.format("%s,%s,%s,%s,%s\n",
                                safeValue(data.lastUpdateId()),
                                topBidPrice,
                                topBidQty,
                                topAskPrice,
                                topAskQty
                        ));
                    }
                }
                log.info("Written {} Depth records to {}", items.size(), csvFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Error writing Depth CSV: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Safely convert any object to string, returning empty string if null
     */
    private String safeValue(Object value) {
        return value == null ? "" : value.toString();
    }

    /**
     * Safely convert string to string, returning empty string if null
     */
    private String safeString(String value) {
        return value == null ? "" : value;
    }
}