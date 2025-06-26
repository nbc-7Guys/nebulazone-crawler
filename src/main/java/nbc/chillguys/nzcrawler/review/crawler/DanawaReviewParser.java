package nbc.chillguys.nzcrawler.review.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.microsoft.playwright.ElementHandle;

import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.review.dto.ReviewInfo;

@Component
@Slf4j
public class DanawaReviewParser {

	public List<ReviewInfo> parse(List<ElementHandle> items, Long catalogId) {
		List<ReviewInfo> result = new ArrayList<>();
		int index = 1;
		for (ElementHandle item : items) {
			parseAndLogReview(result, item, catalogId, index++);
		}
		log.info("📄 [{}] 리뷰 파싱 총 {}건", catalogId, result.size());
		return result;
	}

	// --- 리뷰 파싱/로깅 ---
	private void parseAndLogReview(List<ReviewInfo> result, ElementHandle item, Long catalogId, int index) {
		try {
			String content = extractContent(item);
			int star = extractStar(item);

			if (content != null && !content.isBlank()) {
				ReviewInfo info = ReviewInfo.of(star, catalogId, content.trim());
				result.add(info);
				log.info("✅ [{}-{}] 리뷰 파싱: [별점: {}] {}", catalogId, index, star, preview(content.trim()));
			} else {
				log.warn("⚠️ [{}-{}] 파싱 실패: content가 없음/공백", catalogId, index);
			}
		} catch (Exception e) {
			log.warn("⚠️ [{}-{}] 리뷰 파싱 오류: {}", catalogId, index, e.getMessage());
		}
	}

	private String extractContent(ElementHandle item) {
		return Optional.ofNullable(item.querySelector(".atc"))
			.map(ElementHandle::innerText)
			.orElse(null);
	}

	private int extractStar(ElementHandle item) {
		return Optional.ofNullable(item.querySelector(".star_mask"))
			.map(el -> parseStar(el.getAttribute("style")))
			.orElse(0);
	}

	private int parseStar(String style) {
		try {
			if (style == null) return 0;
			String numbers = style.replaceAll("[^\\d]", "");
			if (numbers.isEmpty()) return 0;
			return Integer.parseInt(numbers) / 20;
		} catch (Exception e) {
			return 0;
		}
	}

	private String preview(String text) {
		if (text == null) return "";
		return text.length() > 30 ? text.substring(0, 30) + "..." : text;
	}
}
