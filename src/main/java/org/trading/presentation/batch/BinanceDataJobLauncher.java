package org.trading.presentation.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.trading.domain.enumeration.Interval;
import org.trading.application.service.batch.BinanceDataJob;

/**
 * Dynamic job launcher that executes batch jobs for all KLineIntervals
 * Runs automatically on application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceDataJobLauncher implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final BinanceDataJob jobConfiguration;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Binance data batch jobs for all intervals...");

        for (Interval interval : Interval.values()) {
            try {
                log.info("Processing interval: {}", interval.getInterval());

                // Execute KLine Job
                executeJob(jobConfiguration.createKLineJob(interval), interval, "KLine");

                // Execute Ticker Job
                executeJob(jobConfiguration.createTickerJob(interval), interval, "Ticker");

                // Execute Depth Job
                executeJob(jobConfiguration.createDepthJob(interval), interval, "Depth");

                log.info("Completed all jobs for interval: {}", interval.getInterval());
            } catch (Exception e) {
                log.error("Error processing interval {}: {}", interval.getInterval(), e.getMessage(), e);
            }
        }

        log.info("All Binance data batch jobs completed!");
    }

    private void executeJob(Job job, Interval interval, String jobType) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("interval", interval.getInterval())
                    .addString("jobType", jobType)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(job, jobParameters);
            log.info("Successfully executed {} job for interval {}", jobType, interval.getInterval());
        } catch (Exception e) {
            log.error("Failed to execute {} job for interval {}: {}", jobType, interval.getInterval(), e.getMessage(), e);
        }
    }
}