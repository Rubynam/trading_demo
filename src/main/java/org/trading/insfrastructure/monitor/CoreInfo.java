package org.trading.insfrastructure.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CoreInfo {

  static {

    System.loadLibrary("metricscpu"); // Load native library
  }

  public static native String getCurrentCpu();

  public String formatLogstring() {
    return String.format("%s/%s",Thread.currentThread().getId(),getCurrentCpu());
  }
}
