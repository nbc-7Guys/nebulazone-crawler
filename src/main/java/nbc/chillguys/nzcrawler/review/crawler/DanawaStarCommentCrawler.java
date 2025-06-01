package nbc.chillguys.nzcrawler.review.crawler;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitUntilState;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.repository.CatalogRepository;
import nbc.chillguys.nzcrawler.review.entity.Review;
import nbc.chillguys.nzcrawler.review.repository.ReviewRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DanawaStarCommentCrawler implements CommandLineRunner {

	private static final int BATCH_SIZE = 10;
	private final ReviewRepository reviewRepository;
	private final CatalogRepository catalogRepository;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
	private final Semaphore semaphore = new Semaphore(3);

	@Override
	public void run(String... args) {
		long startTime = System.currentTimeMillis();
		int page = 0;
		while (true) {
			Pageable pageable = PageRequest.of(page, BATCH_SIZE);
			List<Catalog> batch = catalogRepository.findByProductCodeNotNull(pageable);
			if (batch.isEmpty())
				break;

			for (Catalog catalog : batch) {
				if (catalog.getProductCode() == null) {
					log.warn("❕ [{}] productCode null → skip", catalog.getId());
					continue;
				}
				executor.submit(() -> {
					try {
						semaphore.acquire();
						try (Playwright playwright = Playwright.create()) {
							crawlProduct(playwright, catalog.getId(), catalog.getProductCode().toString());
						}
					} catch (Exception e) {
						log.error("❌ [{}] Playwright 실행 중 예외", catalog.getId(), e);
					} finally {
						semaphore.release();
					}
				});
			}
			page++;
		}

		executor.shutdown();
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.error("❌ 인터럽트 발생", e);
			}
		}

		long endTime = System.currentTimeMillis();
		log.info("🏁 크롤링 완료 ⏱ 총 소요 시간: {}초", (endTime - startTime) / 1000);
	}

	private void crawlProduct(Playwright playwright, Long catalogId, String productCode) {
		int maxRetry = 3, attempt = 0;
		while (attempt < maxRetry) {
			try {
				Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
					.setHeadless(false)
					.setProxy(new Proxy("http://52.231.143.39:8080"))
					.setArgs(List.of(
						"--disable-blink-features=AutomationControlled",
						"--disable-dev-shm-usage",
						"--disable-infobars",
						"--no-sandbox",
						"--disable-extensions",
						"--start-maximized",
						"--disable-background-networking",
						"--disable-default-apps"
					))
				);
				BrowserContext context = browser.newContext(new Browser.NewContextOptions()
					.setUserAgent(getRandomUserAgent())
					.setViewportSize(1280, 800)
					.setLocale("ko-KR")
					.setTimezoneId("Asia/Seoul")
				);

				Page page = context.newPage(); // ✅ 반드시 context에서 생성

				page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
				page.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined });");
				page.addInitScript("Object.defineProperty(navigator, 'languages', { get: () => ['ko-KR', 'ko'] });");
				page.addInitScript("Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });");
				page.addInitScript("Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3] });");

				String url = String.format("https://prod.danawa.com/info/?pcode=%s", productCode);
				page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
				log.info("➡️ [{}] 페이지 이동 완료 시도됨: {}", catalogId, url);
				randomDelay(1000, 2000);

				Locator locator = page.locator("a#danawa-prodBlog-productOpinion-button-tab-companyReview");
				if (locator.count() == 0) {
					log.warn("❌ [{}] 리뷰 탭 없음", catalogId);
					browser.close();
					return;
				}
				locator.scrollIntoViewIfNeeded();
				locator.click();
				page.waitForTimeout(1000);

				Locator reviewContainer = page.locator("#danawa-prodBlog-companyReview-content-list");
				if (!waitForContainer(reviewContainer, catalogId)) {
					browser.close();
					return;
				}

				int pageNum = 1, saveCount = 0;
				while (true) {
					List<ElementHandle> reviewItems = page.querySelectorAll(
						"li.danawa-prodBlog-companyReview-clazz-more");
					if (reviewItems.isEmpty())
						break;

					for (ElementHandle item : reviewItems) {
						String content = Optional.ofNullable(item.querySelector(".atc"))
							.map(ElementHandle::innerText)
							.orElse(null);
						int star = Optional.ofNullable(item.querySelector(".star_mask"))
							.map(span -> parseStar(span.getAttribute("style")))
							.orElse(0);

						if (content == null || content.isBlank())
							continue;
						if (reviewRepository.existsByCatalogIdAndContent(catalogId, content.trim()))
							continue;

						reviewRepository.save(
							Review.builder().catalogId(catalogId).star(star).content(content.trim()).build());
						saveCount++;
					}

					log.info("📄 [{}] 페이지 {} - {}개 저장", catalogId, pageNum, saveCount);
					ElementHandle nextBtn = page.querySelector(".pagination .page_next");
					if (nextBtn == null || !nextBtn.isVisible())
						break;
					nextBtn.click();
					page.waitForTimeout(1500);
					pageNum++;
				}

				browser.close();
				log.info("✅ [{}] 전체 리뷰 저장 완료 - 총 {}개", catalogId, saveCount);
				return;
			} catch (Exception e) {
				attempt++;
				log.warn("⏳ [{}] {}번째 시도 실패: {}", catalogId, attempt, e.getMessage());
				try {
					Thread.sleep(1000L * attempt);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
		}
		log.error("❌ [{}] 최대 재시도 초과 - 크롤링 포기", catalogId);
	}

	private void randomDelay(int minMillis, int maxMillis) {
		try {
			Thread.sleep(minMillis + (long)(Math.random() * (maxMillis - minMillis)));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean waitForContainer(Locator container, Long catalogId) {
		try {
			container.waitFor(new Locator.WaitForOptions().setTimeout(10000));
			return true;
		} catch (Exception e) {
			log.warn("❌ [{}] 리뷰 컨테이너 로딩 실패", catalogId);
			return false;
		}
	}

	private int parseStar(String style) {
		try {
			return Integer.parseInt(style.replaceAll("[^\\d]", "")) / 20;
		} catch (Exception e) {
			return 0;
		}
	}

	private String getRandomUserAgent() {
		List<String> userAgents = List.of(
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.1 Safari/605.1.15",
			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; rv:109.0) Gecko/20100101 Firefox/109.0"
		);
		return userAgents.get((int)(Math.random() * userAgents.size()));
	}

}
