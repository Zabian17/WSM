package com.mycompany.tugas_akhir.api.mobile;

public record MobileApiError(ErrorDetail error) {
    public record ErrorDetail(String code, String message) {}
}
