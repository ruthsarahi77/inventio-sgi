package com.ruth.inventio.security;

import java.io.IOException;
import java.time.Instant;
import com.ruth.inventio.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final JsonMapper mapper = JsonMapper.builder().build();
    @Override public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException ex) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(request,response,HttpStatus.UNAUTHORIZED,"Token ausente, invalido o vencido.");
    }
    @Override public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        write(request,response,HttpStatus.FORBIDDEN,"Operacion no autorizada.");
    }
    private void write(HttpServletRequest request,HttpServletResponse response,HttpStatus status,String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mapper.writeValueAsString(new ApiError(Instant.now(),status.value(),
                status.getReasonPhrase(),message,request.getRequestURI())));
    }
}
