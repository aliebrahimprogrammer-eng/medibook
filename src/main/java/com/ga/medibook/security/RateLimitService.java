package com.ga.medibook.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, RequestCounter> requests =
            new ConcurrentHashMap<>();

    public boolean isAllowed(String key) {

        Instant now = Instant.now();

        RequestCounter counter = requests.computeIfAbsent(
                key,
                ignored -> new RequestCounter(now, 0)
        );

        synchronized (counter) {

            if (now.getEpochSecond()
                    - counter.windowStart().getEpochSecond()
                    >= WINDOW_SECONDS) {

                counter.reset(now);
            }

            if (counter.count() >= MAX_REQUESTS) {
                return false;
            }

            counter.increment();

            return true;
        }
    }

    private static class RequestCounter {

        private Instant start;
        private int requestCount;

        public RequestCounter(Instant start, int requestCount) {
            this.start = start;
            this.requestCount = requestCount;
        }

        public Instant windowStart() {
            return start;
        }

        public int count() {
            return requestCount;
        }

        public void increment() {
            requestCount++;
        }

        public void reset(Instant newStart) {
            start = newStart;
            requestCount = 0;
        }
    }
}