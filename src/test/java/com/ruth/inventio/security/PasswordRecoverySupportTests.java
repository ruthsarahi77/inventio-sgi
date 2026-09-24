package com.ruth.inventio.security;

import com.ruth.inventio.config.PasswordRecoveryProperties;
import com.ruth.inventio.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordRecoverySupportTests {
    @Test void randomTokensAreUniqueAndUrlSafe() {
        var tokens=new HashSet<String>();
        for (int i=0;i<1000;i++) {
            String token=RecoveryTokens.generate();
            assertTrue(token.matches("[A-Za-z0-9_-]{43}")); assertTrue(tokens.add(token));
            assertTrue(RecoveryTokens.matches(token,RecoveryTokens.hash(token)));
        }
        assertFalse(RecoveryTokens.matches("different",RecoveryTokens.hash("original")));
    }

    @Test void configuredUrlIsNotDerivedFromHttpInputAndPreservesExistingQuery() {
        var properties=new PasswordRecoveryProperties("https://app.example/reset?source=mobile&token=old",20);
        String token=RecoveryTokens.generate();
        assertEquals("https://app.example/reset?source=mobile&token="+token,properties.link(token));
        assertTrue(new PasswordRecoveryProperties("http://localhost:4200/reset",15).configured());
        assertFalse(new PasswordRecoveryProperties("",20).configured());
        for (String url:new String[]{"http://public.example/reset","javascript:alert(1)","https://user:secret@app.example/reset", "https://app.example/#reset"}) {
            assertThrows(IllegalStateException.class,() -> new PasswordRecoveryProperties(url,20));
        }
        assertThrows(IllegalStateException.class,() -> new PasswordRecoveryProperties("",60));
    }

    @Test void rateLimitsApplyToUnknownEmailsAndExpireWithoutTrustingIdentity() {
        var clock=mock(Clock.class); Instant now=Instant.parse("2026-09-23T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        var limiter=new RecoveryRateLimiter(clock);
        for(int i=0;i<5;i++) assertTrue(limiter.allowForgot("ip-"+i,"missing@example.test"));
        assertFalse(limiter.allowForgot("different-ip","MISSING@example.test"));
        for(int i=0;i<20;i++) assertTrue(limiter.allowForgot("one-ip","unknown"+i+"@example.test"));
        assertFalse(limiter.allowForgot("one-ip","another@example.test"));
        for(int i=0;i<30;i++) assertTrue(limiter.allowReset("reset-ip"));
        assertFalse(limiter.allowReset("reset-ip"));
        when(clock.instant()).thenReturn(now.plusSeconds(900));
        assertTrue(limiter.allowForgot("one-ip","missing@example.test")); assertTrue(limiter.allowReset("reset-ip"));
    }

    @Test void dispatcherQueuesBeforeLookingUpAccountAndSurvivesOverload() {
        var executor=mock(TaskExecutor.class); var service=mock(PasswordRecoveryService.class);
        var mailer=mock(PasswordResetMailer.class);
        var dispatcher=new PasswordRecoveryDispatcher(executor,service,mailer);
        dispatcher.submit("someone@example.test");
        verifyNoInteractions(service,mailer);
        var task=ArgumentCaptor.forClass(Runnable.class); verify(executor).execute(task.capture());
        var mail=new PasswordResetMail("someone@example.test","https://app.example/reset?token=secret",20);
        when(service.issue("someone@example.test")).thenReturn(Optional.of(mail));
        task.getValue().run(); verify(mailer).send(mail);
        assertFalse(mail.toString().contains("secret"));
        doThrow(new TaskRejectedException("full")).when(executor).execute(any());
        assertDoesNotThrow(() -> dispatcher.submit("unknown@example.test"));
    }

    @Test void smtpMessageContainsOnlyRecoveryLinkAndInstructions() {
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        var sender=mock(JavaMailSender.class); when(provider.getIfAvailable()).thenReturn(sender);
        var mailer=new SmtpPasswordResetMailer(provider,"inventio@example.test","smtp.example.test");
        String token=RecoveryTokens.generate();
        mailer.send(new PasswordResetMail("user@example.test","https://app.example/reset?token="+token,20));
        var message=ArgumentCaptor.forClass(SimpleMailMessage.class); verify(sender).send(message.capture());
        assertEquals("inventio@example.test",message.getValue().getFrom());
        assertArrayEquals(new String[]{"user@example.test"},message.getValue().getTo());
        String text=message.getValue().getText();
        assertNotNull(text); assertTrue(text.contains("20 minutos")); assertTrue(text.contains("ignora"));
        assertTrue(text.contains("token="+token)); assertFalse(text.contains("$2"));
        assertThrows(org.springframework.mail.MailSendException.class,
                () -> new SmtpPasswordResetMailer(provider,"","").send(new PasswordResetMail("user@example.test","",20)));
    }

    @Test void centralizedPolicyAcceptsPassphrasesWithoutCompositionRules() {
        assertTrue(PasswordPolicy.isValid("Una frase larga segura"));
        assertTrue(PasswordPolicy.isValid("abcdefghijkl"));
        for (String weak:new String[]{"", "password1234", "123456789012", " ".repeat(12), "a".repeat(12), "é".repeat(40),
                "a"+" ".repeat(12), "  "+"a".repeat(12)}) {
            assertFalse(PasswordPolicy.isValid(weak));
        }
        assertFalse(PasswordPolicy.isValid(null));
    }
}
