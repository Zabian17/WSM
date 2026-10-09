package com.mycompany.tugas_akhir.service;

import com.mycompany.tugas_akhir.model.User;

public interface ApprovalRouter {
    boolean canReview(User requester, User reviewer);
}
