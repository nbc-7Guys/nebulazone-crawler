package nbc.chillguys.nzcrawler.review.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.review.dto.ReviewInfo;

@Slf4j
@Component
public class DanawaReviewCrawler {

	public List<ReviewInfo> crawl(Browser browser, Catalog catalog) {
		List<ReviewInfo> result = new ArrayList<>();

		try {
			BrowserContext context = browser.newContext(new Browser.NewContextOptions()
				.setUserAgent(getRandomUserAgent())
				.setViewportSize(1280, 800)
				.setLocale("ko-KR")
				.setTimezoneId("Asia/Seoul")
			);

			Page page = context.newPage();

			// navigator 속성 우회
			page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
			page.addInitScript("Object.defineProperty(navigator, 'languages', { get: () => ['ko-KR', 'ko'] });");
			page.addInitScript("Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });");
			page.addInitScript("Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3] });");

			String url = "https://prod.danawa.com/info/?pcode=" + catalog.getProductCode();
			page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
			page.waitForTimeout(1000);

			Locator tab = page.locator("#danawa-prodBlog-productOpinion-button-tab-companyReview");
			if (tab.count() == 0) {
				log.warn("❌ [{}] 리뷰 탭 없음", catalog.getId());
				return result;
			}
			tab.scrollIntoViewIfNeeded();
			tab.click();
			page.waitForTimeout(1000);

			while (true) {
				List<ElementHandle> items = page.querySelectorAll("li.danawa-prodBlog-companyReview-clazz-more");
				if (items.isEmpty())
					break;

				for (ElementHandle item : items) {
					String content = Optional.ofNullable(item.querySelector(".atc"))
						.map(ElementHandle::innerText)
						.orElse(null);
					int star = Optional.ofNullable(item.querySelector(".star_mask"))
						.map(el -> parseStar(el.getAttribute("style")))
						.orElse(0);

					if (content == null || content.isBlank())
						continue;

					result.add(new ReviewInfo(star, content.trim()));
				}

				ElementHandle next = page.querySelector(".pagination .page_next");
				if (next == null || !next.isVisible())
					break;
				next.click();
				page.waitForTimeout(1500);
			}
			log.info("✅ [{}] 크롤링 성공 - {}개", catalog.getId(), result.size());
		} catch (Exception e) {
			log.warn("❌ [{}] 크롤링 실패: {}", catalog.getId(), e.getMessage());
		}
		return result;
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
		return agents.get((int)(Math.random() * agents.size()));
	}
}