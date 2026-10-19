package com.mycompany.tugas_akhir.integration.erp;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccurateRequestGateTest {
    @Test
    void limitsRequestsToEightWithinAnyRollingSecond() {
        AtomicLong clock = new AtomicLong();
        AccurateRequestGate gate = new AccurateRequestGate(clock::get, clock::addAndGet);

        for (int i = 0; i < 8; i++) {
            gate.acquire();
            gate.release();
        }
        gate.acquire();
        gate.release();

        assertEquals(1_000_000_000L, clock.get());
    }

    @Test
    void allowsAtMostEightRequestsInFlight() throws InterruptedException {
        AtomicLong clock = new AtomicLong();
        AccurateRequestGate gate = new AccurateRequestGate(clock::get, clock::addAndGet);
        ExecutorService pool = Executors.newFixedThreadPool(9);
        CountDownLatch acquired = new CountDownLatch(8);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch ninthStarted = new CountDownLatch(1);
        CountDownLatch ninthAcquired = new CountDownLatch(1);
        try {
            for (int i = 0; i < 8; i++) {
                pool.submit(() -> {
                    gate.acquire();
                    acquired.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } finally {
                        gate.release();
                    }
                });
            }
            assertTrue(acquired.await(1, TimeUnit.SECONDS));
            pool.submit(() -> {
                ninthStarted.countDown();
                gate.acquire();
                try {
                    ninthAcquired.countDown();
                } finally {
                    gate.release();
                }
            });
            assertTrue(ninthStarted.await(1, TimeUnit.SECONDS));
            assertFalse(ninthAcquired.await(100, TimeUnit.MILLISECONDS));
            release.countDown();
            assertTrue(ninthAcquired.await(1, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }
}
