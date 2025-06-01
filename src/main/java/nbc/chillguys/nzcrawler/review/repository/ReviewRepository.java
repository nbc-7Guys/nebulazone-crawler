package nbc.chillguys.nzcrawler.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nbc.chillguys.nzcrawler.review.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
	boolean existsByCatalogIdAndContent(Long catalogId, String content);

}