package nbc.chillguys.nzcrawler.review.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.repository.CatalogRepository;
import nbc.chillguys.nzcrawler.review.service.DanawaReviewService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DanawaReviewRunner implements CommandLineRunner {

	private static final int THREAD_COUNT = 2; // ✅ 2개 스레드
	private static final int MIN_DELAY = 20000; // 20초 (병렬이므로 조금 증가)
	private static final int MAX_ADDITIONAL_DELAY = 10000; // 10초

	private final CatalogRepository catalogRepository;
	private final DanawaReviewService reviewService;

	@Override
	public void run(String... args) {
		long start = System.currentTimeMillis();

		List<Catalog> allCatalogs = getAllCatalogs();

		// ✅ 360번째부터 시작 (인덱스는 359부터)
		int startIndex = 359; // 360번째는 인덱스 359
		if (startIndex >= allCatalogs.size()) {
			log.warn("⚠️ 시작 인덱스({})가 전체 카탈로그 수({})보다 큽니다.", startIndex, allCatalogs.size());
			return;
		}

		List<Catalog> targetCatalogs = allCatalogs.subList(startIndex, allCatalogs.size());
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

		log.info("🚀 다나와 리뷰 크롤링 시작 (병렬 처리: {}개 스레드, 총 {}개)",
			THREAD_COUNT, allCatalogs.size());

		AtomicInteger processedCount = new AtomicInteger(0);

		for (int i = 0; i < targetCatalogs.size(); i++) {
			Catalog catalog = targetCatalogs.get(i);
			final int globalIndex = startIndex + i; // 전체 인덱스

			executor.submit(() -> {
				try {
					int threadId = (int)(Thread.currentThread().getId() % THREAD_COUNT);
					int initialDelay = threadId * 5000;
					Thread.sleep(initialDelay);

					int delay = MIN_DELAY + (int)(Math.random() * MAX_ADDITIONAL_DELAY);
					Thread.sleep(delay);

					log.info("🔍 [스레드-{}] 크롤링 시작: {} ({}/{})",
						threadId, catalog.getProductCode(), globalIndex + 1, allCatalogs.size());

					reviewService.crawlAndSaveSingle(catalog);

					int completed = processedCount.incrementAndGet();
					log.info("✅ [스레드-{}] 완료: {} (진행률: {}/{}, 전체: {}/{})",
						threadId, catalog.getProductCode(),
						completed, targetCatalogs.size(), globalIndex + 1, allCatalogs.size());

				} catch (Exception e) {
					log.error("❌ [스레드-{}] 크롤링 실패: {} - {}",
						Thread.currentThread().getId() % THREAD_COUNT,
						catalog.getProductCode(), e.getMessage());

					try {
						Thread.sleep(30000 + (int)(Math.random() * 15000));
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			});
		}

		executor.shutdown();
		try {
			executor.awaitTermination(24, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		long end = System.currentTimeMillis();
		log.info("🏁 크롤링 완료 ⏱ {}시간", (end - start) / 1000 / 3600);
	}

	private List<Catalog> getAllCatalogs() {
		List<Catalog> allCatalogs = new ArrayList<>();
		int page = 0;

		while (true) {
			Pageable pageable = PageRequest.of(page, 100);
			List<Catalog> batch = catalogRepository.findByProductCodeNotNull(pageable);
			if (batch.isEmpty())
				break;

			allCatalogs.addAll(batch);
			page++;
		}

		return allCatalogs;
	}
}
