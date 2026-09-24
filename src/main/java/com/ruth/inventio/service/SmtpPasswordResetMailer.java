package com.ruth.inventio.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpPasswordResetMailer implements PasswordResetMailer {
    private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetMailer.class);
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final String host;
    public SmtpPasswordResetMailer(ObjectProvider<JavaMailSender> sender,
            @Value("${MAIL_FROM:}") String from, @Value("${MAIL_HOST:}") String host) {
        this.sender = sender; this.from = from; this.host = host;
    }
    @EventListener(ApplicationReadyEvent.class)
    public void reportMissingConfiguration(ApplicationReadyEvent event) {
        var environment = event.getApplicationContext().getEnvironment();
        boolean authentication = environment.getProperty("MAIL_SMTP_AUTH", Boolean.class, true);
        var missing = List.of("MAIL_HOST", "MAIL_PORT", "MAIL_USERNAME", "MAIL_PASSWORD", "MAIL_FROM",
                        "FRONTEND_RESET_PASSWORD_URL").stream()
                .filter(name -> authentication || (!name.equals("MAIL_USERNAME") && !name.equals("MAIL_PASSWORD")))
                .filter(name -> !StringUtils.hasText(environment.getProperty(name, name.equals("MAIL_PORT") ? "587" : "")))
                .toList();
        if (!missing.isEmpty()) {
            log.warn("Recuperacion de contrasena: configurar las variables de entorno {}. MAIL_PORT usa 587 por defecto. No se muestran valores de configuracion.",
                    String.join(", ", missing));
        }
    }
    @Override public void send(PasswordResetMail mail) {
        JavaMailSender transport = sender.getIfAvailable();
        if (transport == null || from.isBlank() || host.isBlank()) throw new MailSendException("SMTP no configurado.");
        var message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(mail.recipient());
        message.setSubject("Inventio - Restablecer contraseña");
        message.setText("Se solicitó restablecer la contraseña de tu cuenta de Inventio.\n\n"
                + "Para establecer una nueva contraseña, abre este enlace:\n" + mail.link()
                + "\n\nEl enlace expira en " + mail.ttlMinutes() + " minutos y solo puede utilizarse una vez.\n"
                + "Si no realizaste esta solicitud, ignora este mensaje. Tu contraseña no ha cambiado.\n\nEquipo Inventio");
        transport.send(message);
    }
}
