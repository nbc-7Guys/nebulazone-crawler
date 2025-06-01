package nbc.chillguys.nzcrawler.review.crawler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.repository.CatalogRepository;
import nbc.chillguys.nzcrawler.review.service.DanawaReviewService;

@Component
@RequiredArgsConstructor
@Slf4j
public class DanawaReviewRunner implements CommandLineRunner {

	private static final int BATCH_SIZE = 10;
	private final CatalogRepository catalogRepository;
	private final DanawaReviewService reviewService;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
	private final Semaphore semaphore = new Semaphore(3);

	@Override
	public void run(String... args) {
		long start = System.currentTimeMillis();

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
				))
			);

			int page = 0;
			while (true) {
				Pageable pageable = PageRequest.of(page, BATCH_SIZE);
				List<Catalog> batch = catalogRepository.findByProductCodeNotNull(pageable);
				if (batch.isEmpty())
					break;

				for (Catalog catalog : batch) {
					executor.submit(() -> {
						try {
							semaphore.acquire();
							reviewService.crawlAndSaveOne(browser, catalog);
						} catch (Exception e) {
							log.error("❌ [{}] 처리 중 오류 발생", catalog.getId(), e);
						} finally {
							semaphore.release();
						}
					});
				}
				page++;
			}

			executor.shutdown();
			while (!executor.isTerminated()) {
				Thread.sleep(500);
			}

			browser.close();
			long end = System.currentTimeMillis();
			log.info("🏁 전체 리뷰 크롤링 완료 ⏱ 소요 시간: {}초", (end - start) / 1000);

		} catch (Exception e) {
			log.error("❌ 전체 크롤링 실패", e);
		}
	}
}
