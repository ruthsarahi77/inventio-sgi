package com.ruth.inventio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.time.Clock;

@Configuration
public class PasswordRecoveryConfig {
    @Bean("passwordRecoveryClock") public Clock passwordRecoveryClock() { return Clock.systemUTC(); }
    // No sustituir el executor general que Spring Boot ofrece a MVC/otros modulos.
    @Bean(name="passwordRecoveryExecutor", defaultCandidate=false)
    public ThreadPoolTaskExecutor passwordRecoveryExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); executor.setMaxPoolSize(2); executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("password-recovery-");
        executor.setWaitForTasksToCompleteOnShutdown(true); executor.setAwaitTerminationSeconds(15);
        return executor;
    }
}
