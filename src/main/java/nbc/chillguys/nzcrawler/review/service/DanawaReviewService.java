package nbc.chillguys.nzcrawler.review.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.review.crawler.DanawaReviewCrawler;
import nbc.chillguys.nzcrawler.review.dto.ReviewInfo;
import nbc.chillguys.nzcrawler.review.entity.Review;
import nbc.chillguys.nzcrawler.review.repository.ReviewRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DanawaReviewService {

	private final ReviewRepository reviewRepository;
	private final DanawaReviewCrawler crawler;

	public void crawlAndSaveReviews(Catalog catalog) {
		List<ReviewInfo> reviews = crawler.crawl(catalog);
		if (reviews.isEmpty()) {
			log.info("ℹ️ [{}] 크롤링된 리뷰 없음", catalog.getId());
			return;
		}

		// ✅ catalogId에 해당하는 기존 리뷰 content만 조회해서 Set으로 보관
		Set<String> existingContents = new HashSet<>(
			reviewRepository.findContentsByCatalogId(catalog.getId())
		);

		int saveCount = 0;
		for (ReviewInfo info : reviews) {
			String content = info.content();
			if (existingContents.contains(content))
				continue;

			reviewRepository.save(
				Review.builder()
					.catalogId(catalog.getId())
					.star(info.star())
					.content(content)
					.build()
			);
			saveCount++;
		}

		if (saveCount == 0) {
			log.info("ℹ️ [{}] 중복으로 인해 저장된 리뷰 없음", catalog.getId());
		} else {
			log.info("✅ [{}] 저장 완료 - {}개", catalog.getId(), saveCount);
		}
	}
}