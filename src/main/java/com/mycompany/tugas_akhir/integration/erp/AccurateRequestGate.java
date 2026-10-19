package com.mycompany.tugas_akhir.integration.erp;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

final class AccurateRequestGate {
    private static final int MAX_REQUESTS_PER_SECOND = 8;
    private static final int MAX_IN_FLIGHT = 8;
    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(1);

    private final Semaphore inFlight = new Semaphore(MAX_IN_FLIGHT);
    private final Deque<Long> requestStarts = new ArrayDeque<>();
    private final LongSupplier nanoClock;
    private final NanoSleeper sleeper;

    AccurateRequestGate() {
        this(System::nanoTime, nanos -> TimeUnit.NANOSECONDS.sleep(nanos));
    }

    AccurateRequestGate(LongSupplier nanoClock, NanoSleeper sleeper) {
        this.nanoClock = nanoClock;
        this.sleeper = sleeper;
    }

    void acquire() {
        boolean acquired = false;
        try {
            inFlight.acquire();
            acquired = true;
            acquireRateSlot();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (acquired) inFlight.release();
            throw new IllegalStateException("Accurate request was interrupted.", exception);
        } catch (RuntimeException exception) {
            if (acquired) inFlight.release();
            throw exception;
        }
    }

    void release() {
        inFlight.release();
    }

    private void acquireRateSlot() throws InterruptedException {
        while (true) {
            long delay;
            synchronized (requestStarts) {
                long now = nanoClock.getAsLong();
                while (!requestStarts.isEmpty() && now - requestStarts.peekFirst() >= WINDOW_NANOS) {
                    requestStarts.removeFirst();
                }
                if (requestStarts.size() < MAX_REQUESTS_PER_SECOND) {
                    requestStarts.addLast(now);
                    return;
                }
                delay = WINDOW_NANOS - (now - requestStarts.peekFirst());
            }
            sleeper.sleep(delay);
        }
    }

    @FunctionalInterface
    interface NanoSleeper {
        void sleep(long nanos) throws InterruptedException;
    }
}
