package com.mycompany.tugas_akhir.service;

import java.time.LocalDate;
import java.time.YearMonth;

public record DashboardPeriod(LocalDate from, LocalDate to) {
    public DashboardPeriod {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Rentang tanggal dashboard tidak valid.");
        }
    }

    public static DashboardPeriod lastDays(int days, LocalDate today) {
        if (days < 1 || days > 366 || today == null) {
            throw new IllegalArgumentException("Periode harus antara 1 dan 366 hari.");
        }
        return new DashboardPeriod(today.minusDays(days - 1L), today);
    }

    public static DashboardPeriod currentMonth(LocalDate today) {
        if (today == null) throw new IllegalArgumentException("Tanggal hari ini wajib diisi.");
        return new DashboardPeriod(YearMonth.from(today).atDay(1), today);
    }
}
