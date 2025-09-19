package com.example.prgi.repo;

import com.example.prgi.domain.ApplicationEntity;
import com.example.prgi.domain.ModuleType;
import com.example.prgi.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    List<ApplicationEntity> findByModuleTypeAndStatusAndSubmittedDateBetween(
            ModuleType moduleType, Status status, LocalDate from, LocalDate to);

    // ✅ Needed for Excel export
    List<ApplicationEntity> findBySubmittedDateBetween(LocalDate fromDate, LocalDate toDate);

    @Query("""
        SELECT a.moduleType, a.status, COUNT(a)
        FROM ApplicationEntity a
        WHERE a.submittedDate BETWEEN :from AND :to
        GROUP BY a.moduleType, a.status
    """)
    List<Object[]> aggregateByModuleAndStatus(LocalDate from, LocalDate to);
}
