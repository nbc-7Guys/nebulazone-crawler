package nbc.chillguys.nzcrawler.review.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import nbc.chillguys.nzcrawler.review.crawler.DanawaReviewRunner;

@Slf4j
@Component
@RequiredArgsConstructor
public class DanawaScheduler {

	private final DanawaReviewRunner reviewRunner;

	@Async
	@Scheduled(cron = "0 30 0 * * *")
	public void execute() {
		log.info("⏰ DanawaReviewScheduler - 크롤링 작업 시작");
		try {
			reviewRunner.startCrawlingProcess();
			log.info("✅ DanawaReviewScheduler - 크롤링 작업 완료");
		} catch (Exception e) {
			log.error("❌ DanawaReviewScheduler - 크롤링 작업 실패", e);
		}
	}
}