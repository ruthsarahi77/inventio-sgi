package com.ruth.inventio.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class EntidadAuditable extends EntidadCreada {

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void registrarActualizacionInicial() {
        updatedAt = getCreatedAt();
    }

    @PreUpdate
    protected void registrarActualizacion() {
        updatedAt = Instant.now();
    }
}
