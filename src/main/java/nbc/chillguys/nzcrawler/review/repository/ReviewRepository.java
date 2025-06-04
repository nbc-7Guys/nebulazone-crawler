package nbc.chillguys.nzcrawler.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nbc.chillguys.nzcrawler.review.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
	boolean existsByCatalogIdAndContent(Long catalogId, String content);

	@Query("SELECT r.content FROM Review r WHERE r.catalogId = :catalogId")
	List<String> findContentsByCatalogId(@Param("catalogId") Long catalogId);
}