package nbc.chillguys.nzcrawler.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nbc.chillguys.nzcrawler.product.entity.Catalog;

public interface CatalogRepository extends JpaRepository<Catalog, Long> {

	@Query("SELECT MAX(c.productCode) FROM Catalog c")
	Optional<Integer> findMaxProductCode();

	@Query("SELECT c FROM Catalog c WHERE c.productCode IS NOT NULL")
	List<Catalog> findByProductCodeNotNull(Pageable pageable);

	long countByProductCodeNotNull();
}
