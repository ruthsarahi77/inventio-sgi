package com.ruth.inventio.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Limite local acotado. En despliegues con replicas, complementar en el proxy/gateway. */
@Component
public class RecoveryRateLimiter {
    private static final int MAX_KEYS=10_000;
    private static final long WINDOW_SECONDS=900;
    private final Clock clock;
    private final Map<String,Bucket> buckets=new HashMap<>();
    public RecoveryRateLimiter(@Qualifier("passwordRecoveryClock") Clock clock) { this.clock=clock; }
    public synchronized boolean allowForgot(String ip, String email) {
        Instant now=clock.instant(); cleanup(now);
        // Todas las direcciones, existan o no, consumen cuota. No almacenar emails sin hash.
        return take("forgot-ip:"+ip,20,now)
                && take("email:"+RecoveryTokens.hash(email.trim().toLowerCase(Locale.ROOT)),5,now);
    }
    public synchronized boolean allowReset(String ip) {
        Instant now=clock.instant(); cleanup(now);
        return take("reset-ip:"+ip,30,now);
    }
    private void cleanup(Instant now) { buckets.values().removeIf(b -> !b.until.isAfter(now)); }
    private boolean take(String key,int limit,Instant now) {
        Bucket bucket=buckets.get(key);
        if (bucket==null) {
            if (buckets.size()>=MAX_KEYS) return false;
            bucket=new Bucket(now.plusSeconds(WINDOW_SECONDS)); buckets.put(key,bucket);
        }
        if (bucket.count>=limit) return false;
        bucket.count++; return true;
    }
    private static final class Bucket {
        private final Instant until;
        private int count;
        private Bucket(Instant until) { this.until=until; }
    }
}
