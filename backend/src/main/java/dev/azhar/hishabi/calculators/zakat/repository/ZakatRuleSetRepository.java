package dev.azhar.hishabi.calculators.zakat.repository;

import dev.azhar.hishabi.calculators.zakat.model.ZakatRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZakatRuleSetRepository extends JpaRepository<ZakatRuleSet, Long> {

    Optional<ZakatRuleSet> findFirstByOrderByIdAsc();
}
