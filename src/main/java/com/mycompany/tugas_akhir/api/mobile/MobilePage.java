package com.mycompany.tugas_akhir.api.mobile;

import java.util.List;

public record MobilePage<T>(List<T> data, Pagination pagination) {
    public record Pagination(int page, int pageSize, long totalItems, int totalPages) {}
}
