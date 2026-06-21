package dev.azhar.hishabi.calculators.zakat.repository;

import dev.azhar.hishabi.calculators.zakat.model.Metal;
import dev.azhar.hishabi.calculators.zakat.model.MetalPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetalPriceRepository extends JpaRepository<MetalPrice, Long> {

    Optional<MetalPrice> findByMetalAndCarat(Metal metal, String carat);
}
