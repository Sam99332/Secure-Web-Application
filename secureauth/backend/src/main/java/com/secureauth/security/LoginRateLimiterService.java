package com.secureauth.security;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 300;
    private static final long LOCKOUT_SECONDS = 120;

    private static class AttemptBucket {
        final List<Instant> failures = new ArrayList<>();
        Instant lockedUntil = Instant.EPOCH;
    }

    private final Map<String, AttemptBucket> buckets = new ConcurrentHashMap<>();

    public long getRemainingLockoutSeconds(String key) {
        AttemptBucket bucket = buckets.get(key.toLowerCase());
        if (bucket == null) return 0;
        synchronized (bucket) {
            Instant now = Instant.now();
            if (bucket.lockedUntil.isAfter(now)) {
                return Math.max(1, bucket.lockedUntil.getEpochSecond() - now.getEpochSecond());
            }
            return 0;
        }
    }

    public long recordFailure(String key) {
        AttemptBucket bucket = buckets.computeIfAbsent(key.toLowerCase(), k -> new AttemptBucket());
        synchronized (bucket) {
            Instant now = Instant.now();
            Instant cutoff = now.minusSeconds(WINDOW_SECONDS);
            bucket.failures.removeIf(t -> t.isBefore(cutoff));
            bucket.failures.add(now);
            if (bucket.failures.size() >= MAX_ATTEMPTS) {
                bucket.lockedUntil = now.plusSeconds(LOCKOUT_SECONDS);
                bucket.failures.clear();
                return LOCKOUT_SECONDS;
            }
            return 0;
        }
    }

    public void reset(String key) {
        buckets.remove(key.toLowerCase());
    }
}
