package com.ruth.inventio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

/** Consulta y SMTP fuera del hilo HTTP: no expone la existencia de cuentas por su tiempo de envio. */
@Service
public class PasswordRecoveryDispatcher {
    private static final Logger log=LoggerFactory.getLogger(PasswordRecoveryDispatcher.class);
    private final TaskExecutor executor;
    private final PasswordRecoveryService service;
    private final PasswordResetMailer mailer;
    public PasswordRecoveryDispatcher(@Qualifier("passwordRecoveryExecutor") TaskExecutor executor,
            PasswordRecoveryService service, PasswordResetMailer mailer) {
        this.executor=executor; this.service=service; this.mailer=mailer;
    }
    public void submit(String email) {
        try {
            executor.execute(() -> {
                try { service.issue(email).ifPresent(mailer::send); }
                catch (RuntimeException ex) {
                    // No registrar excepcion: los proveedores SMTP pueden incluir destinatario o mensaje.
                    log.warn("No se pudo completar una solicitud de recuperacion. Revisar disponibilidad de BD/SMTP.");
                }
            });
        } catch (TaskRejectedException ex) {
            log.warn("Cola de recuperacion ocupada. Solicitud omitida.");
        }
    }
}
