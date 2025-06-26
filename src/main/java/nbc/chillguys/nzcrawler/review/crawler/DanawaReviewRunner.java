package nbc.chillguys.nzcrawler.review.crawler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.repository.CatalogRepository;
import nbc.chillguys.nzcrawler.review.service.DanawaReviewService;

import com.microsoft.playwright.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DanawaReviewRunner {

	// --- 환경설정 값 외부화 ---
	@Value("${crawler.thread.count:3}")
	private int threadCount;

	@Value("${crawler.start.index:0}")
	private int startIndex;

	private static final int MIN_DELAY_MS = 20000;
	private static final int MAX_ADDITIONAL_DELAY_MS = 10000;
	private static final int CATALOG_BATCH_SIZE = 100;

	private final CatalogRepository catalogRepository;
	private final DanawaReviewService reviewService;

	/**
	 * 외부(스케줄러 등)에서 명시적으로 호출할 크롤링 시작 메서드
	 */
	public void startCrawlingProcess() {
		long start = System.currentTimeMillis();
		List<Catalog> allCatalogs = getAllCatalogs();

		if (startIndex >= allCatalogs.size()) {
			log.warn("⚠️ 시작 인덱스({})가 전체 카탈로그 수({})보다 큽니다.", startIndex, allCatalogs.size());
			return;
		}

		List<Catalog> targetCatalogs = allCatalogs.subList(startIndex, allCatalogs.size());
		log.info("🚀 다나와 리뷰 크롤링 시작 (병렬: {}스레드, 타겟: {}개)", threadCount, targetCatalogs.size());

		try (Playwright playwright = Playwright.create();
			 Browser browser = createBrowser(playwright)) {

			ExecutorService executor = Executors.newFixedThreadPool(threadCount);
			AtomicInteger processedCount = new AtomicInteger(0);

			for (int i = 0; i < targetCatalogs.size(); i++) {
				Catalog catalog = targetCatalogs.get(i);
				int globalIndex = startIndex + i;
				executor.submit(() -> {
					try (BrowserContext context = createBrowserContext(browser)) {
						Page page = createPage(context);
						submitCrawlingTask(page, catalog, globalIndex, allCatalogs.size(), targetCatalogs.size(), processedCount);
					} catch (Exception e) {
						log.error("❌ BrowserContext 생성/종료 중 에러: {}", e.getMessage(), e);
					}
				});
			}

			shutdownAndAwait(executor);

		} catch (Exception e) {
			log.error("❌ Playwright 전체 처리 중 에러: {}", e.getMessage(), e);
		}

		long end = System.currentTimeMillis();
		Duration duration = Duration.ofMillis(end - start);
		String formattedDuration = String.format("%d시간 %d분 %d초",
			duration.toHours(),
			duration.toMinutesPart(),
			duration.toSecondsPart());
		log.info("🏁 크롤링 전체 완료. 총 소요 시간: {}", formattedDuration);
	}

	// 개별 카탈로그 크롤링 Job
	private void submitCrawlingTask(
		Page page, Catalog catalog, int globalIndex, int totalSize, int targetSize, AtomicInteger processedCount
	) {
		int threadId = (int)(Thread.currentThread().getId() % threadCount);
		try {
			Thread.sleep(threadId * 5000L); // 스레드별 시작 시간 분산
			log.info("🔍 [스레드-{}] 크롤링 시작: {} ({}/{})",
				threadId, catalog.getProductCode(), globalIndex + 1, totalSize);

			reviewService.crawlAndSaveSingle(page, catalog);

			int completed = processedCount.incrementAndGet();
			log.info("✅ [스레드-{}] 완료: {} (진행률: {}/{}, 전체: {}/{})",
				threadId, catalog.getProductCode(),
				completed, targetSize, globalIndex + 1, totalSize);

		} catch (Exception e) {
			handleCrawlError(threadId, catalog, e);
		} finally {
			sleepRandomDelay();
		}
	}

	private void handleCrawlError(int threadId, Catalog catalog, Exception e) {
		log.error("❌ [스레드-{}] 크롤링 작업 실패: {} - {}",
			threadId, catalog.getProductCode(), e.getMessage());
	}

	private void shutdownAndAwait(ExecutorService executor) {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(24, TimeUnit.HOURS)) {
				log.warn("⏳ Executor가 24시간 내에 종료되지 않았습니다. 강제 종료를 시도합니다.");
				executor.shutdownNow();
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					log.error("❌ Executor가 강제 종료되지 않았습니다.");
				}
			}
		} catch (InterruptedException e) {
			log.error("⏳ 종료 대기 중 인터럽트 발생. 강제 종료를 시도합니다.", e);
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	// 전체 Catalog 배치 단위 페이징 조회
	private List<Catalog> getAllCatalogs() {
		List<Catalog> allCatalogs = new ArrayList<>();
		int page = 0;
		while (true) {
			Pageable pageable = PageRequest.of(page, CATALOG_BATCH_SIZE);
			List<Catalog> batch = catalogRepository.findByProductCodeNotNull(pageable);
			if (batch.isEmpty())
				break;
			allCatalogs.addAll(batch);
			page++;
		}
		return allCatalogs;
	}

	private Browser createBrowser(Playwright playwright) {
		return playwright.chromium().launch(
			new BrowserType.LaunchOptions()
				.setHeadless(true)
				.setArgs(List.of(
					"--disable-blink-features=AutomationControlled",
					"--disable-dev-shm-usage",
					"--no-sandbox",
					"--disable-extensions"
				))
		);
	}

	private BrowserContext createBrowserContext(Browser browser) {
		return browser.newContext(
			new Browser.NewContextOptions()
				.setUserAgent(getRandomUserAgent())
				.setViewportSize(1920, 1080)
				.setLocale("ko-KR")
				.setTimezoneId("Asia/Seoul")
				.setExtraHTTPHeaders(java.util.Map.of(
					"Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8",
					"Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
				))
		);
	}

	private Page createPage(BrowserContext context) {
		Page page = context.newPage();
		page.setDefaultTimeout(60000);
		return page;
	}

	private void sleepRandomDelay() {
		try {
			int delay = ThreadLocalRandom.current().nextInt(MIN_DELAY_MS, MIN_DELAY_MS + MAX_ADDITIONAL_DELAY_MS);
			Thread.sleep(delay);
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
		return agents.get((int) (Math.random() * agents.size()));
	}
}
