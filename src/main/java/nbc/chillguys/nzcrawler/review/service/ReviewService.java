package nbc.chillguys.nzcrawler.review.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import nbc.chillguys.nzcrawler.review.entity.Review;
import nbc.chillguys.nzcrawler.review.repository.ReviewRepository;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;

	public void saveReview(Long catalogId, String content) {
		Review review = Review.builder()
			.star(0)
			.catalogId(catalogId)
			.content(content)
			.build();

		reviewRepository.save(review);
	}
}
