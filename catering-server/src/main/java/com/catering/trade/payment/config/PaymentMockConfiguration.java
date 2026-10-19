package com.catering.trade.payment.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

/**
 * Mock 支付需要的异步调度器与 HTTP 客户端。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "catering.payment",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true
)
public class PaymentMockConfiguration {

    @Bean
    public RestTemplate paymentMockRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public TaskScheduler paymentMockTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("mock-wechat-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);
        scheduler.initialize();
        return scheduler;
    }
}
