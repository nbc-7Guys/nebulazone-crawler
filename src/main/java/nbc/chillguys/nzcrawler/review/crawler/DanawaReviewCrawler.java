package nbc.chillguys.nzcrawler.review.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

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

	// DanawaReviewCrawler.java에서 page.waitForTimeout 사용을 Thread.sleep으로 변경
	public List<ReviewInfo> crawl(Page page, Catalog catalog) {
		List<ReviewInfo> result = new ArrayList<>();
		String url = "https://prod.danawa.com/info/?pcode=" + catalog.getProductCode();

		for (int attempt = 1; attempt <= 3; attempt++) {
			try {
				log.info("🔍 [{}] {}번째 시도 시작", catalog.getId(), attempt);

				page.addInitScript("""
                Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
                Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});
                Object.defineProperty(navigator, 'languages', {get: () => ['ko-KR', 'ko', 'en']});
                Object.defineProperty(navigator, 'permissions', {get: () => undefined});
                window.chrome = { runtime: {} };
                Object.defineProperty(navigator, 'connection', {get: () => undefined});
            """);

				page.navigate(url, new Page.NavigateOptions()
					.setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
					.setTimeout(60000));

				// ✅ page.waitForTimeout 대신 Thread.sleep 사용
				Thread.sleep(5000 + (int)(Math.random() * 3000));

				try {
					page.waitForSelector("body", new Page.WaitForSelectorOptions().setTimeout(10000));
				} catch (Exception e) {
					log.warn("⚠️ [{}] body 요소 대기 실패", catalog.getId());
				}

				Locator tab = page.locator("#danawa-prodBlog-productOpinion-button-tab-companyReview");

				try {
					tab.waitFor(new Locator.WaitForOptions().setTimeout(15000));
				} catch (Exception e) {
					log.warn("⚠️ [{}] 리뷰 탭을 찾을 수 없음", catalog.getId());
					continue;
				}

				if (!tab.isVisible()) {
					log.warn("⚠️ [{}] 리뷰 탭이 보이지 않음", catalog.getId());
					continue;
				}

				tab.scrollIntoViewIfNeeded();
				Thread.sleep(2000); // ✅ page.waitForTimeout → Thread.sleep
				tab.click();
				Thread.sleep(5000 + (int)(Math.random() * 3000)); // ✅ page.waitForTimeout → Thread.sleep

				// 리뷰 수집 로직...
				int pageCount = 0;
				int maxPages = 5;

				while (pageCount < maxPages) {
					try {
						page.waitForSelector("li.danawa-prodBlog-companyReview-clazz-more",
							new Page.WaitForSelectorOptions().setTimeout(10000));
					} catch (Exception e) {
						log.info("ℹ️ [{}] 리뷰 요소 대기 시간 초과", catalog.getId());
						break;
					}

					List<ElementHandle> items = page.querySelectorAll("li.danawa-prodBlog-companyReview-clazz-more");

					if (items.isEmpty()) {
						log.info("ℹ️ [{}] 리뷰 없음", catalog.getId());
						break;
					}

					for (ElementHandle item : items) {
						try {
							String content = Optional.ofNullable(item.querySelector(".atc"))
								.map(ElementHandle::innerText)
								.orElse(null);

							int star = Optional.ofNullable(item.querySelector(".star_mask"))
								.map(el -> parseStar(el.getAttribute("style")))
								.orElse(0);

							if (content != null && !content.isBlank()) {
								result.add(new ReviewInfo(star, content.trim()));
							}
						} catch (Exception e) {
							log.warn("⚠️ [{}] 리뷰 파싱 오류: {}", catalog.getId(), e.getMessage());
						}
					}

					ElementHandle nextButton = page.querySelector(".pagination .page_next");
					if (nextButton == null || !nextButton.isVisible()) {
						log.info("ℹ️ [{}] 마지막 페이지 도달", catalog.getId());
						break;
					}

					nextButton.click();
					Thread.sleep(5000 + (int)(Math.random() * 3000)); // ✅ page.waitForTimeout → Thread.sleep
					pageCount++;
				}

				log.info("✅ [{}] 크롤링 성공 - {}개 리뷰, {}페이지",
					catalog.getId(), result.size(), pageCount + 1);
				return result;

			} catch (Exception e) {
				log.warn("⏳ [{}] {}번째 시도 실패: {}", catalog.getId(), attempt, e.getMessage());

				if (attempt < 3) {
					try {
						int retryDelay = 10000 + (int)(Math.random() * 10000);
						log.info("⏳ [{}] {}초 후 재시도", catalog.getId(), retryDelay / 1000);
						Thread.sleep(retryDelay);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}

		log.error("❌ [{}] 최대 재시도 초과", catalog.getId());
		return result;
	}


	private int parseStar(String style) {
		try {
			if (style == null)
				return 0;
			String numbers = style.replaceAll("[^\\d]", "");
			if (numbers.isEmpty())
				return 0;
			return Integer.parseInt(numbers) / 20;
		} catch (Exception e) {
			return 0;
		}
	}
}
