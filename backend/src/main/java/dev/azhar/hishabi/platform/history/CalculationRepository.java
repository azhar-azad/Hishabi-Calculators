package dev.azhar.hishabi.platform.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationRepository extends JpaRepository<Calculation, Long> {

    Page<Calculation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Calculation> findByUserIdAndCalculatorTypeOrderByCreatedAtDesc(
            Long userId, CalculatorType calculatorType, Pageable pageable);
}
