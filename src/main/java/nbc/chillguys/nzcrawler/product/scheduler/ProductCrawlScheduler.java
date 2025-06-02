package nbc.chillguys.nzcrawler.product.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import nbc.chillguys.nzcrawler.product.service.ProductCrawlService;

@RequiredArgsConstructor
@Component
public class ProductCrawlScheduler {

	private final ProductCrawlService productCrawlService;

	@Scheduled(cron = "0 0 0 * * *")
	public void execute() {
		productCrawlService.execute();
	}

}
