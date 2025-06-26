package nbc.chillguys.nzcrawler.review.crawler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.entity.Catalog;

@Slf4j
@Component
public class DanawaReviewCrawler {

	// --- 상수 정의 ---
	private static final String BASE_URL = "https://prod.danawa.com/info/?pcode=";
	private static final int MAX_RETRY_ATTEMPTS = 3;
	private static final int MAX_PAGES_TO_CRAWL = 5; // 최대 크롤링할 페이지 수

	// 선택자(Selectors)
	private static final String REVIEW_TAB_SELECTOR = "#danawa-prodBlog-productOpinion-button-tab-companyReview";
	private static final String REVIEW_LIST_SELECTOR = "ul.danawa-prodBlog-companyReview-list";
	private static final String REVIEW_ITEM_SELECTOR = "li.danawa-prodBlog-companyReview-clazz-more";
	private static final String NEXT_PAGE_BUTTON_SELECTOR = ".pagination .page_next";

	// 타임아웃(Timeouts)
	private static final int NAVIGATION_TIMEOUT = 60000;
	private static final int DEFAULT_WAIT_TIMEOUT = 15000;
	private static final int RETRY_DELAY_BASE = 10000;
	private static final int RETRY_DELAY_RANDOM = 10000;

	public List<ElementHandle> crawl(Page page, Catalog catalog) {
		String url = BASE_URL + catalog.getProductCode();

		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				log.info("🔍 [{}] {}번째 시도 시작", catalog.getId(), attempt);
				moveToReviewTab(page, url);
				List<ElementHandle> result = collectAllReviews(page, catalog.getId());
				log.info("✅ [{}] 크롤링 성공 - {}개 리뷰", catalog.getId(), result.size());
				return result;
			} catch (Exception e) {
				log.warn("⏳ [{}] {}번째 시도 실패: {}", catalog.getId(), attempt, e.getMessage());
				handleRetryOrBreak(attempt, catalog.getId());
			}
		}
		log.error("❌ [{}] 최대 재시도 초과", catalog.getId());
		return List.of();
	}

	private void moveToReviewTab(Page page, String url) {
		addStealthInitScript(page);
		page.navigate(url, new Page.NavigateOptions()
			.setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
			.setTimeout(NAVIGATION_TIMEOUT));

		Locator tab = page.locator(REVIEW_TAB_SELECTOR);
		tab.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_WAIT_TIMEOUT));
		if (!tab.isVisible()) {
			throw new IllegalStateException("리뷰 탭이 보이지 않음");
		}
		tab.scrollIntoViewIfNeeded();
		tab.click();

		page.waitForSelector(REVIEW_LIST_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(DEFAULT_WAIT_TIMEOUT));
	}

	private List<ElementHandle> collectAllReviews(Page page, Long catalogId) {
		List<ElementHandle> result = new ArrayList<>();
		for (int pageCount = 0; pageCount < MAX_PAGES_TO_CRAWL; pageCount++) {
			List<ElementHandle> items = getCurrentReviewItems(page, catalogId);
			if (items.isEmpty()) {
				break;
			}
			result.addAll(items);
			if (!moveToNextPage(page, catalogId)) {
				break;
			}
		}
		return result;
	}

	private List<ElementHandle> getCurrentReviewItems(Page page, Long catalogId) {
		try {
			page.waitForSelector(REVIEW_ITEM_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(DEFAULT_WAIT_TIMEOUT));
		} catch (Exception e) {
			log.info("ℹ️ [{}] 리뷰 요소 대기 시간 초과 (리뷰가 없거나 로딩 실패)", catalogId);
			return List.of();
		}
		List<ElementHandle> items = page.querySelectorAll(REVIEW_ITEM_SELECTOR);
		if (items.isEmpty()) {
			log.info("ℹ️ [{}] 현재 페이지에 리뷰 없음", catalogId);
		}
		return items;
	}



	private boolean moveToNextPage(Page page, Long catalogId) {
		Locator nextButton = page.locator(NEXT_PAGE_BUTTON_SELECTOR);
		if (!nextButton.isVisible()) {
			log.info("ℹ️ [{}] 마지막 페이지 도달", catalogId);
			return false;
		}

		Locator lastReview = page.locator(REVIEW_ITEM_SELECTOR).last();
		String lastReviewIdBeforeClick = lastReview.getAttribute("id");

		nextButton.click();

		try {
			page.waitForCondition(() ->
					!page.locator("#" + lastReviewIdBeforeClick).isVisible(),
				new Page.WaitForConditionOptions().setTimeout(DEFAULT_WAIT_TIMEOUT)
			);
		} catch (Exception e) {
			log.warn("ℹ️ [{}] 다음 페이지 로딩 확인 중 타임아웃. 마지막 페이지일 수 있습니다.", catalogId);
			return false;
		}
		return true;
	}

	private void addStealthInitScript(Page page) {
		// (내용 동일)
		page.addInitScript("""
            Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
            Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});
            Object.defineProperty(navigator, 'languages', {get: () => ['ko-KR', 'ko', 'en']});
            Object.defineProperty(navigator, 'permissions', {get: () => undefined});
            window.chrome = { runtime: {} };
            Object.defineProperty(navigator, 'connection', {get: () => undefined});
        """);
	}

	private void handleRetryOrBreak(int attempt, Long catalogId) {
		if (attempt < MAX_RETRY_ATTEMPTS) {
			try {
				int retryDelay = RETRY_DELAY_BASE + (int)(Math.random() * RETRY_DELAY_RANDOM);
				log.info("⏳ [{}] {}초 후 재시도", catalogId, retryDelay / 1000);
				Thread.sleep(retryDelay);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
	}
}