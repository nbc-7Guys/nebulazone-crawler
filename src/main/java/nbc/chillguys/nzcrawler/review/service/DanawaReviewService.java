package nbc.chillguys.nzcrawler.review.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;

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

	private final DanawaReviewCrawler crawler;
	private final ReviewRepository reviewRepository;

	public void crawlAndSaveOne(Browser browser, Catalog catalog) {
		List<ReviewInfo> reviews = crawler.crawl(browser, catalog);
		int saveCount = 0;

		for (ReviewInfo info : reviews) {
			String content = info.content();
			if (reviewRepository.existsByCatalogIdAndContent(catalog.getId(), content))
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
			log.info("ℹ️ [{}] 저장할 리뷰 없음", catalog.getId());
		} else {
			log.info("✅ [{}] 저장 완료 - {}개", catalog.getId(), saveCount);
		}
	}
}
