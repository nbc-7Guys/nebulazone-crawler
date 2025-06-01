package nbc.chillguys.nzcrawler.review.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.repository.CatalogRepository;
import nbc.chillguys.nzcrawler.review.crawler.DanawaStarCommentCrawler;
import nbc.chillguys.nzcrawler.review.entity.Review;
import nbc.chillguys.nzcrawler.review.repository.ReviewRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class DanawaReviewService {

	private final ReviewRepository reviewRepository;

	public void crawlAndSaveReview(Catalog catalog) {
		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
				.setHeadless(false)
				.setArgs(List.of("--disable-blink-features=AutomationControlled"))
			);

			BrowserContext context = browser.newContext(new Browser.NewContextOptions()
				.setUserAgent(getRandomUserAgent())
			);

			Page page = context.newPage();
			String url = "https://prod.danawa.com/info/?pcode=" + catalog.getProductCode();

			page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
			page.waitForTimeout(1000);

			Locator tab = page.locator("#danawa-prodBlog-productOpinion-button-tab-companyReview");
			if (tab.count() == 0) {
				log.warn("❌ [{}] 리뷰 탭 없음", catalog.getId());
				return;
			}

			tab.click();
			page.waitForTimeout(1000);

			int saveCount = 0;
			while (true) {
				List<ElementHandle> items = page.querySelectorAll("li.danawa-prodBlog-companyReview-clazz-more");
				for (ElementHandle item : items) {
					String content = Optional.ofNullable(item.querySelector(".atc")).map(ElementHandle::innerText).orElse(null);
					int star = Optional.ofNullable(item.querySelector(".star_mask")).map(el -> parseStar(el.getAttribute("style"))).orElse(0);

					if (content == null || content.isBlank()) continue;
					if (reviewRepository.existsByCatalogIdAndContent(catalog.getId(), content.trim())) continue;

					reviewRepository.save(
						Review.builder().catalogId(catalog.getId()).star(star).content(content.trim()).build()
					);
					saveCount++;
				}

				ElementHandle next = page.querySelector(".pagination .page_next");
				if (next == null || !next.isVisible()) break;
				next.click();
				page.waitForTimeout(1500);
			}
			log.info("✅ [{}] 저장 완료 - {}개", catalog.getId(), saveCount);
			browser.close();
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
		List<String> agents = List.of(
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X)..."
		);
		return agents.get((int) (Math.random() * agents.size()));
	}
}
