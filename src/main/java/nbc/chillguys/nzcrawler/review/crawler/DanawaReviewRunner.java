package nbc.chillguys.nzcrawler.review.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.repository.CatalogRepository;
import nbc.chillguys.nzcrawler.review.service.DanawaReviewService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class DanawaReviewRunner implements CommandLineRunner {

	private static final int BATCH_SIZE = 10;
	private final CatalogRepository catalogRepository;
	private final DanawaReviewService reviewService;

	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
	private final Semaphore semaphore = new Semaphore(1);

	@Override
	public void run(String... args) {
		long startTime = System.currentTimeMillis();

		// 기본값 설정
		int start = 0;
		int end = Integer.MAX_VALUE;

		// 인자 파싱
		for (String arg : args) {
			if (arg.startsWith("--start=")) {
				start = Integer.parseInt(arg.substring("--start=".length()));
			} else if (arg.startsWith("--end=")) {
				end = Integer.parseInt(arg.substring("--end=".length()));
			}
		}
		log.info("📦 크롤링 범위: start={} ~ end={}", start, end);

		int page = 0;
		while (true) {
			int offset = page * BATCH_SIZE;
			if (offset > end) break; // end 범위 초과 시 종료

			Pageable pageable = PageRequest.of(page, BATCH_SIZE);
			List<Catalog> batch = catalogRepository.findByProductCodeNotNull(pageable);
			if (batch.isEmpty()) break;

			// 현재 offset이 start 이전이면 skip
			if (offset + BATCH_SIZE < start) {
				page++;
				continue;
			}

			executor.submit(() -> {
				try {
					semaphore.acquire();
					reviewService.crawlAndSaveAll(batch);
				} catch (Exception e) {
					log.error("❌ 리뷰 저장 중 오류 발생", e);
				} finally {
					semaphore.release();
				}
			});

			page++;
		}

		executor.shutdown();
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		long endTime = System.currentTimeMillis();
		log.info("🏁 전체 리뷰 크롤링 완료 ⏱ 소요 시간: {}초", (endTime - startTime) / 1000);
	}

}
