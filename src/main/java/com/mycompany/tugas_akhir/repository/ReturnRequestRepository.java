package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.ReturnRequest;
import com.mycompany.tugas_akhir.model.ReturnStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Integer> {

    List<ReturnRequest> findTop200ByOrderByCreatedAtDesc();

    Page<ReturnRequest> findByStatusOrderByCreatedAtAsc(ReturnStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT r FROM ReturnRequest r LEFT JOIN FETCH r.lines WHERE r.id = :id")
    Optional<ReturnRequest> findByIdWithLinesForUpdate(@Param("id") Integer id);

    @Query("SELECT COALESCE(SUM(line.quantity), 0) FROM ReturnRequestLine line "
            + "WHERE line.purchaseOrderLine.id = :sourceLineId "
            + "AND line.returnRequest.status IN :statuses")
    long sumPurchaseReturnQuantity(@Param("sourceLineId") Integer sourceLineId,
                                   @Param("statuses") List<ReturnStatus> statuses);

    @Query("SELECT COALESCE(SUM(line.quantity), 0) FROM ReturnRequestLine line "
            + "WHERE line.deliveryOrderLine.id = :sourceLineId "
            + "AND line.returnRequest.status IN :statuses")
    long sumSalesReturnQuantity(@Param("sourceLineId") Integer sourceLineId,
                                @Param("statuses") List<ReturnStatus> statuses);
}
