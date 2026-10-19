package com.mycompany.tugas_akhir.api.mobile;

final class MobileApiExceptions {
    private MobileApiExceptions() {}

    static final class InvalidCredentials extends RuntimeException {}
    static final class RateLimited extends RuntimeException {}
    static final class NotFound extends RuntimeException {}
}
