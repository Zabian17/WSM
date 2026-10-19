package com.mycompany.tugas_akhir.repository;

import com.mycompany.tugas_akhir.model.PickTask;
import com.mycompany.tugas_akhir.model.PickTaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PickTaskRepository extends JpaRepository<PickTask, Integer> {

    @Query("SELECT t FROM PickTask t WHERE t.assignedPicker.id = :pickerId AND t.status = :status "
            + "AND t.deliveryOrder IS NOT NULL ORDER BY t.createdAt ASC")
    Page<PickTask> findMobileTasks(@Param("pickerId") Integer pickerId,
                                   @Param("status") PickTaskStatus status, Pageable pageable);

    @Query("SELECT t FROM PickTask t WHERE t.assignedPicker.id = :pickerId "
            + "AND t.materialUsage IS NOT NULL AND t.status IN ("
            + "com.mycompany.tugas_akhir.model.PickTaskStatus.ASSIGNED, "
            + "com.mycompany.tugas_akhir.model.PickTaskStatus.IN_PROGRESS) ORDER BY t.createdAt ASC")
    Page<PickTask> findMobileMaterialUsageTasks(@Param("pickerId") Integer pickerId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PickTask t WHERE t.id = :id")
    Optional<PickTask> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT t FROM PickTask t WHERE t.deliveryOrder.id = :orderId "
            + "AND t.status IN (com.mycompany.tugas_akhir.model.PickTaskStatus.OPEN, "
            + "com.mycompany.tugas_akhir.model.PickTaskStatus.ASSIGNED, "
            + "com.mycompany.tugas_akhir.model.PickTaskStatus.IN_PROGRESS) "
            + "ORDER BY t.createdAt DESC")
    List<PickTask> findActiveByOrderId(@Param("orderId") Integer orderId);

    @Query("SELECT t FROM PickTask t WHERE t.materialUsage.id = :requestId "
            + "AND t.status IN (com.mycompany.tugas_akhir.model.PickTaskStatus.OPEN, "
            + "com.mycompany.tugas_akhir.model.PickTaskStatus.ASSIGNED, "
            + "com.mycompany.tugas_akhir.model.PickTaskStatus.IN_PROGRESS) "
            + "ORDER BY t.createdAt DESC")
    List<PickTask> findActiveByMaterialUsageId(@Param("requestId") Integer requestId);

    List<PickTask> findTop200ByOrderByCreatedAtDesc();
}
