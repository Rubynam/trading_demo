package org.trading.insfrastructure.config;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@EnableAsync
@Component
@RequiredArgsConstructor
public class CustomTomcatConfig {

  private final ServerProperties serverProperties;


  @Bean
  public TomcatServletWebServerFactory tomcatProtocolHandlerCustomizer() {
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

    factory.addConnectorCustomizers(connector -> {
      Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
      protocol.setExecutor(threadPoolExecutor());
    });


    return factory;
  }

  private ThreadPoolExecutor threadPoolExecutor() {
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    ThreadFactory threadFactory = r -> {
      var thread = new Thread(r);
      thread.setName("Tomcat-thread-%d");
      return thread;
    };
    return new ThreadPoolExecutor(
        serverProperties.getTomcat().getThreads().getMinSpare(),
        serverProperties.getTomcat().getThreads().getMax(),
        60,                      // Keep Alive Time (giây)
        TimeUnit.SECONDS,        // Time Unit
        queue,
        threadFactory
    );
  }



}
