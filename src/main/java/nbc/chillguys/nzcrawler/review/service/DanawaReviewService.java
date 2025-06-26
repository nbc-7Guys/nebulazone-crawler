package nbc.chillguys.nzcrawler.review.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.review.crawler.DanawaReviewCrawler;
import nbc.chillguys.nzcrawler.review.crawler.DanawaReviewParser;
import nbc.chillguys.nzcrawler.review.dto.ReviewInfo;
import nbc.chillguys.nzcrawler.review.entity.Review;
import nbc.chillguys.nzcrawler.review.repository.ReviewEsRepository;
import nbc.chillguys.nzcrawler.review.repository.ReviewRepository;
import nbc.chillguys.nzcrawler.review.vo.ReviewDocument;

@Slf4j
@Service
@RequiredArgsConstructor
public class DanawaReviewService {

	private final DanawaReviewCrawler crawler;
	private final DanawaReviewParser parser;
	private final ReviewRepository reviewRepository;
	private final ReviewEsRepository reviewEsRepository;

	/**
	 * Page 객체를 받아 단일 카탈로그에 대한 크롤링 및 저장을 수행합니다.
	 * 브라우저/Playwright 생명주기는 Runner에서 관리합니다.
	 * @param page    Playwright Page 객체
	 * @param catalog 크롤링할 카탈로그
	 */
	public void crawlAndSaveSingle(Page page, Catalog catalog) {
		try {
			List<ElementHandle> items = crawler.crawl(page, catalog);
			List<ReviewInfo> reviews = parser.parse(items, catalog.getId());
			saveReviews(catalog, reviews);
		} catch (Exception e) {
			log.error("❌ [{}] 크롤링 실패", catalog.getId(), e);
			throw new RuntimeException("크롤링 실패: " + catalog.getId(), e);
		}
	}

	private void saveReviews(Catalog catalog, List<ReviewInfo> reviews) {
		Set<String> existingContents = new HashSet<>(reviewRepository.findContentsByCatalogId(catalog.getId()));

		List<Review> newReviews = reviews.stream()
			.filter(info -> !existingContents.contains(info.content()))
			.map(Review::from)
			.collect(Collectors.toList());

		if (!newReviews.isEmpty()) {
			// 1. DB에 먼저 저장
			reviewRepository.saveAll(newReviews);
			// 2. ES에도 저장
			List<ReviewDocument> esDocs = newReviews.stream()
				.map(ReviewDocument::from)
				.collect(Collectors.toList());
			reviewEsRepository.saveAll(esDocs);
		}

		logSaveResult(catalog.getId(), newReviews.size());
	}

	private void logSaveResult(Long catalogId, int saveCount) {
		if (saveCount == 0) {
			log.info("ℹ️ [{}] 중복 제외, 저장할 새 리뷰 없음", catalogId);
		} else {
			log.info("✅ [{}] 새 리뷰 {}개 저장 완료", catalogId, saveCount);
		}
	}
}
