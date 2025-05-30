package nbc.chillguys.nzcrawler.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import nbc.chillguys.nzcrawler.product.entity.Catalog;

public interface CatalogRepository extends JpaRepository<Catalog, Long> {
}
