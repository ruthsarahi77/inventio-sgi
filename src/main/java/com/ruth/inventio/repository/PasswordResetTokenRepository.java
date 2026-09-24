package com.ruth.inventio.repository;

import com.ruth.inventio.entity.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends BaseRepository<PasswordResetToken> {
    @Query("select t.usuario.id from PasswordResetToken t where t.tokenHash = :hash")
    Optional<Long> findUsuarioIdByTokenHash(@Param("hash") String hash);
    Optional<PasswordResetToken> findByUsuarioId(Long usuarioId);
}
