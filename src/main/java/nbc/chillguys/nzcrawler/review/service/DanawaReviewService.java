package nbc.chillguys.nzcrawler.review.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

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

	public void crawlAndSaveAll(List<Catalog> catalogs) {
		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
				.setHeadless(false)
				.setArgs(List.of(
					"--disable-blink-features=AutomationControlled",
					"--disable-dev-shm-usage",
					"--disable-infobars",
					"--no-sandbox",
					"--disable-extensions",
					"--start-maximized",
					"--disable-background-networking",
					"--disable-default-apps"
				)));

			BrowserContext context = browser.newContext(new Browser.NewContextOptions()
				.setUserAgent(getRandomUserAgent())
				.setViewportSize(1280, 800)
				.setLocale("ko-KR")
				.setTimezoneId("Asia/Seoul")
			);

			for (Catalog catalog : catalogs) {
				Page page = context.newPage();
				List<ReviewInfo> reviews = crawler.crawl(page, catalog);
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

				if (saveCount == 0) {
					log.info("ℹ️ [{}] 저장할 리뷰 없음", catalog.getId());
				} else {
					log.info("✅ [{}] 저장 완료 - {}개", catalog.getId(), saveCount);
				}

				page.close(); // 페이지는 명시적으로 닫기
			}

			context.close();
			browser.close();

		} catch (Exception e) {
			log.error("❌ 전체 리뷰 저장 중 오류 발생", e);
		}
	}

	private String getRandomUserAgent() {
		List<String> agents = List.of(
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X)..."
		);
		return agents.get((int)(Math.random() * agents.size()));
	}
}
