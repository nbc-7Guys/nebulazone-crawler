package nbc.chillguys.nzcrawler.review.crawler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.repository.CatalogRepository;
import nbc.chillguys.nzcrawler.review.service.DanawaReviewService;

@Component
@RequiredArgsConstructor
@Slf4j
public class DanawaStarCommentCrawler implements CommandLineRunner {

	private static final int BATCH_SIZE = 10;
	private final CatalogRepository catalogRepository;
	private final DanawaReviewService danawaReviewService;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
	private final Semaphore semaphore = new Semaphore(3);

	@Override
	public void run(String... args) {
		long start = System.currentTimeMillis();
		int page = 0;

		while (true) {
			Pageable pageable = PageRequest.of(page, BATCH_SIZE);
			List<Catalog> catalogs = catalogRepository.findByProductCodeNotNull(pageable);
			if (catalogs.isEmpty())
				break;

			for (Catalog catalog : catalogs) {
				if (catalog.getProductCode() == null)
					continue;

				executor.submit(() -> {
					try {
						semaphore.acquire();
						danawaReviewService.crawlAndSaveReview(catalog);
					} catch (Exception e) {
						log.error("❌ [{}] 크롤링 실패", catalog.getId(), e);
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
				Thread.sleep(300);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		log.info("🏁 전체 크롤링 완료 ⏱ {}초", (System.currentTimeMillis() - start) / 1000);
	}
}
