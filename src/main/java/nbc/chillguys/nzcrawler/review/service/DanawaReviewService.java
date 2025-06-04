package nbc.chillguys.nzcrawler.review.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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

	// ✅ 공유 브라우저 제거 - 각 호출마다 독립적으로 생성

	public void crawlAndSaveSingle(Catalog catalog) {
		// ✅ 각 호출마다 독립적인 Playwright 인스턴스 생성
		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
				.setHeadless(true)
				.setProxy(new Proxy("socks5://127.0.0.1:1080"))
				.setArgs(List.of(
					"--disable-blink-features=AutomationControlled",
					"--disable-dev-shm-usage",
					"--no-sandbox",
					"--disable-extensions"
				)));

			try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
				.setUserAgent(getRandomUserAgent())
				.setViewportSize(1920, 1080)
				.setLocale("ko-KR")
				.setTimezoneId("Asia/Seoul")
				.setExtraHTTPHeaders(Map.of(
					"Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8",
					"Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
				))
			)) {

				Page page = context.newPage();
				page.setDefaultTimeout(60000);

				List<ReviewInfo> reviews = crawler.crawl(page, catalog);
				saveReviews(catalog, reviews);

				// ✅ 대기 시간을 try-catch 밖으로 이동

			} finally {
				browser.close();
			}
		} catch (Exception e) {
			log.error("❌ [{}] 크롤링 실패", catalog.getId(), e);
			throw new RuntimeException("크롤링 실패", e);
		}

		// ✅ 브라우저 종료 후 대기 (안전)
		try {
			Thread.sleep(2000 + (int)(Math.random() * 3000));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private String getRandomUserAgent() {
		List<String> agents = List.of(
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/120.0.0.0 Safari/537.36"
		);
		return agents.get((int)(Math.random() * agents.size()));
	}

	private void saveReviews(Catalog catalog, List<ReviewInfo> reviews) {
		int saveCount = 0;
		for (ReviewInfo info : reviews) {
			String content = info.content();
			if (reviewRepository.existsByCatalogIdAndContent(catalog.getId(), content)) {
				continue;
			}

			reviewRepository.save(Review.builder()
				.catalogId(catalog.getId())
				.star(info.star())
				.content(content)
				.build());
			saveCount++;
		}

		if (saveCount == 0) {
			log.info("ℹ️ [{}] 중복 제외, 저장할 리뷰 없음", catalog.getId());
		} else {
			log.info("✅ [{}] 새 리뷰 {}개 저장 완료", catalog.getId(), saveCount);
		}
	}
}

