package dev.azhar.hishabi.calculators.tax.repository;

import dev.azhar.hishabi.calculators.tax.model.AssessmentYear;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentYearRepository extends JpaRepository<AssessmentYear, Long> {

    Optional<AssessmentYear> findByLabel(String label);

    /** Latest assessment year by label. Lexicographic order is correct for the "YYYY-YY" format. */
    Optional<AssessmentYear> findTopByOrderByLabelDesc();
}
