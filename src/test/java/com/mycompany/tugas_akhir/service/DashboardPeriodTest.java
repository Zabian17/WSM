package com.mycompany.tugas_akhir.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardPeriodTest {
    @Test
    void resolvesTrailingDaysIncludingToday() {
        LocalDate today = LocalDate.of(2026, 10, 19);

        assertEquals(new DashboardPeriod(LocalDate.of(2026, 10, 13), today),
                DashboardPeriod.lastDays(7, today));
    }

    @Test
    void resolvesCurrentMonthFromItsFirstDayThroughToday() {
        LocalDate today = LocalDate.of(2026, 10, 19);

        assertEquals(new DashboardPeriod(LocalDate.of(2026, 10, 1), today),
                DashboardPeriod.currentMonth(today));
    }

    @Test
    void rejectsInvalidRangesAndDurations() {
        assertThrows(IllegalArgumentException.class,
                () -> new DashboardPeriod(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 19)));
        assertThrows(IllegalArgumentException.class,
                () -> DashboardPeriod.lastDays(0, LocalDate.of(2026, 10, 19)));
    }
}
