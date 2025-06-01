package nbc.chillguys.nzcrawler.review.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

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

		int saveCount = 0;
		for (ReviewInfo info : reviews) {
			String content = info.content();
			if (reviewRepository.existsByCatalogIdAndContent(catalog.getId(), content)) continue;

			reviewRepository.save(
				Review.builder()
					.catalogId(catalog.getId())
					.star(info.star())
					.content(content)
					.build()
			);
			saveCount++;
		}

		log.info("✅ [{}] 저장 완료 - {}개", catalog.getId(), saveCount);
	}
}
