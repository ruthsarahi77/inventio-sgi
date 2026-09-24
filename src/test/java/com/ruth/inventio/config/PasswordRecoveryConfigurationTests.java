package com.ruth.inventio.config;

import com.ruth.inventio.repository.PasswordResetTokenRepository;
import com.ruth.inventio.repository.UsuarioRepository;
import com.ruth.inventio.security.RecoveryRateLimiter;
import com.ruth.inventio.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.Duration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PasswordRecoveryConfigurationTests {
    private final ApplicationContextRunner runner=new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,MailSenderAutoConfiguration.class))
            .withUserConfiguration(PasswordRecoveryConfig.class,PasswordRecoveryProperties.class,PasswordRecoveryService.class,
                    PasswordRecoveryDispatcher.class,SmtpPasswordResetMailer.class,RecoveryRateLimiter.class)
            .withBean(UsuarioRepository.class,() -> mock(UsuarioRepository.class))
            .withBean(PasswordResetTokenRepository.class,() -> mock(PasswordResetTokenRepository.class))
            .withBean(PasswordEncoder.class,() -> new BCryptPasswordEncoder(4));

    @Test void startsWithoutMailSettingsAndKeepsBootExecutorAvailable() {
        runner.withPropertyValues("inventio.password-reset.frontend-url=", "spring.mail.host=").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PasswordRecoveryDispatcher.class)
                    .hasSingleBean(SmtpPasswordResetMailer.class);
            assertThat(context.getBean(PasswordRecoveryProperties.class).configured()).isFalse();
            assertThat(context.getBean("passwordRecoveryExecutor")).isNotSameAs(context.getBean("applicationTaskExecutor"));
        });
    }

    @Test void startsWithMailSettingsWithoutConnectingOrSending() {
        runner.withPropertyValues("spring.mail.host=smtp.example.test", "spring.mail.port=587",
                "inventio.password-reset.frontend-url=https://app.example/reset",
                "MAIL_HOST=smtp.example.test", "MAIL_FROM=inventio@example.test").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(JavaMailSender.class)
                    .hasSingleBean(SmtpPasswordResetMailer.class);
            assertThat(context.getBean(PasswordRecoveryProperties.class).configured()).isTrue();
        });
    }

    @Test @ExtendWith(OutputCaptureExtension.class)
    void missingSmtpConfigurationIsReportedWithoutExposingValues(CapturedOutput output) {
        runner.withPropertyValues("MAIL_HOST=", "MAIL_PORT=", "MAIL_USERNAME=", "MAIL_PASSWORD=",
                "MAIL_FROM=", "FRONTEND_RESET_PASSWORD_URL=", "MAIL_SMTP_AUTH=true").run(context -> {
            context.publishEvent(new ApplicationReadyEvent(new SpringApplication(),new String[0],
                    context.getSourceApplicationContext(),Duration.ZERO));
            assertThat(output.getOut()).contains("MAIL_HOST", "MAIL_PORT", "MAIL_USERNAME", "MAIL_PASSWORD",
                    "MAIL_FROM", "FRONTEND_RESET_PASSWORD_URL");
        });
        runner.withPropertyValues("MAIL_HOST=smtp.example.test", "MAIL_USERNAME=sample-user-not-for-logs",
                "MAIL_PASSWORD=sample-secret-not-for-logs", "MAIL_FROM=", "FRONTEND_RESET_PASSWORD_URL=").run(context -> {
            context.publishEvent(new ApplicationReadyEvent(new SpringApplication(),new String[0],
                    context.getSourceApplicationContext(),Duration.ZERO));
            assertThat(output.getAll()).doesNotContain("sample-user-not-for-logs", "sample-secret-not-for-logs");
        });
    }
}
