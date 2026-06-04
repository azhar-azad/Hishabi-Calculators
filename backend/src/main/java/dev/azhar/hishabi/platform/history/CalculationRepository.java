package dev.azhar.hishabi.platform.history;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationRepository extends JpaRepository<Calculation, Long> {

    List<Calculation> findByUserId(Long userId);

    Page<Calculation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
