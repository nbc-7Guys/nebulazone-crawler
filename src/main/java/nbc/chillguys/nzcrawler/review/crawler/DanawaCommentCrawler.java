package nbc.chillguys.nzcrawler.review.crawler;
//
// import java.util.List;
// import java.util.Optional;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
//
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;
//
// import com.microsoft.playwright.Browser;
// import com.microsoft.playwright.BrowserType;
// import com.microsoft.playwright.ElementHandle;
// import com.microsoft.playwright.Page;
// import com.microsoft.playwright.Playwright;
// import com.microsoft.playwright.options.LoadState;
//
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import nbc.chillguys.nzcrawler.review.entity.Review;
// import nbc.chillguys.nzcrawler.review.repository.ReviewRepository;
//
// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class DanawaCommentCrawler implements CommandLineRunner {
//
// 	private final ReviewRepository reviewRepository;
// 	// Java 21 이상에서 사용 가능
// 	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
//
//
// 	@Override
// 	public void run(String... args) {
// 		// 수집 대상: catalogId(내부 DB용 식별자) + Danawa 상품 URL
// 		List<ProductSource> productSources = List.of(
// 			new ProductSource(62794082L, "https://prod.danawa.com/info/?pcode=62653715&bookmark=cm_opinion")
// 		);
//
// 		// 각 상품별로 병렬 수집 실행
// 		for (ProductSource source : productSources) {
// 			executor.submit(() -> {
// 				try (Playwright playwright = Playwright.create()) {
// 					// pcode를 URL에서 추출
// 					String pcode = extractProductCodeFromUrl(source.danawaUrl());
// 					if (pcode == null) {
// 						log.warn("❕ [{}] pcode 추출 실패", source.catalogId());
// 						return;
// 					}
// 					crawlProduct(playwright, source.catalogId(), pcode);
// 				}
// 			});
// 		}
// 	}
//
// 	// URL에서 pcode=xxxxxx 형태의 값을 정규식으로 추출
// 	private String extractProductCodeFromUrl(String url) {
// 		Matcher matcher = Pattern.compile("pcode=(\\d+)").matcher(url);
// 		return matcher.find() ? matcher.group(1) : null;
// 	}
//
// 	// Playwright를 이용한 단일 상품 리뷰 크롤링 로직
// 	private void crawlProduct(Playwright playwright, Long catalogId, String productCode) {
// 		try {
// 			// 브라우저/페이지 초기화
// 			Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
// 			Page page = browser.newPage();
//
// 			// 해당 상품의 리뷰 페이지로 이동
// 			String url = String.format("https://prod.danawa.com/info/?pcode=%s&bookmark=cm_opinion", productCode);
// 			page.navigate(url);
// 			page.waitForLoadState(LoadState.DOMCONTENTLOADED);
//
// 			// 쇼핑몰 상품리뷰 탭 선택 (필수 클릭 요소)
// 			ElementHandle tabAnchor;
// 			try {
// 				tabAnchor = page.waitForSelector(
// 					"a#danawa-prodBlog-productOpinion-button-tab-companyReview",
// 					new Page.WaitForSelectorOptions().setTimeout(15000)
// 				);
// 			} catch (Exception e) {
// 				log.warn("❕ [{}] 리뷰 탭 대기 중 Timeout 또는 예외 발생", catalogId);
// 				return;
// 			}
//
// 			if (tabAnchor == null) {
// 				log.warn("❕ [{}] 쇼핑몰 리뷰 탭이 존재하지 않음 (querySelector 결과 null)", catalogId);
// 				return;
// 			}
//
// 			// 해당 탭이 이미 활성화되어 있는지 확인하고, 아니면 클릭
// 			ElementHandle parentLi = tabAnchor.evaluateHandle("node => node.closest('li')").asElement();
// 			String className = parentLi.getAttribute("class");
// 			if (className == null || !className.contains("on")) {
// 				tabAnchor.click();
// 				page.waitForTimeout(1200); // 탭 전환 대기
// 			}
//
// 			int pageNum = 1;
// 			int saveCount = 0;
//
// 			// 페이지 순회하며 리뷰 추출
// 			while (true) {
// 				List<ElementHandle> reviewItems = page.querySelectorAll("li.danawa-prodBlog-companyReview-clazz-more");
//
// 				if (reviewItems.isEmpty()) {
// 					log.info("❕ [{}] 리뷰 없음 (페이지 {})", catalogId, pageNum);
// 					break;
// 				}
//
// 				for (ElementHandle item : reviewItems) {
// 					// 리뷰 본문 텍스트
// 					String content = Optional.ofNullable(item.querySelector(".atc"))
// 						.map(ElementHandle::innerText)
// 						.orElse(null);
//
// 					// 별점 정보 (스타일에서 % 추출 → 20 기준 나눔)
// 					int star = Optional.ofNullable(item.querySelector(".star_mask"))
// 						.map(span -> span.getAttribute("style"))
// 						.map(style -> {
// 							try {
// 								return Integer.parseInt(style.replaceAll("[^\\d]", "")) / 20;
// 							} catch (Exception e) {
// 								return 0;
// 							}
// 						}).orElse(0);
//
// 					if (content == null || content.isBlank()) continue;
//
// 					// DB 저장 객체 생성 및 저장
// 					Review review = Review.builder()
// 						.catalogId(catalogId)
// 						.star(star)
// 						.content(content.trim())
// 						.build();
//
// 					reviewRepository.save(review);
// 					saveCount++;
// 				}
//
// 				// 다음 페이지 이동
// 				ElementHandle nextBtn = page.querySelector(".pagination .page_next");
// 				if (nextBtn == null || !nextBtn.isVisible()) break;
//
// 				nextBtn.click();
// 				page.waitForTimeout(1500); // 페이지 로딩 대기
// 				pageNum++;
// 			}
//
// 			// 완료 로그
// 			browser.close();
// 			log.info("✅ 상품 코드 [{}] - 리뷰 {}개 저장 완료", catalogId, saveCount);
//
// 		} catch (Exception e) {
// 			log.warn("❌ [{}] 리뷰 크롤링 중 예외 발생", catalogId, e);
// 		}
// 	}
//
// 	// 상품 정보 구조체: 내부 식별자와 원본 다나와 URL
// 	public record ProductSource(Long catalogId, String danawaUrl) {}
// }