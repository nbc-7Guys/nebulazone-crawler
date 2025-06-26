package nbc.chillguys.nzcrawler.review.dto;

import java.time.LocalDateTime;

/**
 * Danawa 리뷰 파싱/전송용 불변 DTO.
 * (엔티티와 분리되어 파싱~서비스~저장 흐름에서 안전하게 사용)
 */
public record ReviewInfo(
	Integer star,
	Long catalogId,
	String content,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {

	public static ReviewInfo of(Integer star, Long catalogId, String content) {
		LocalDateTime now = LocalDateTime.now();
		return new ReviewInfo(star, catalogId, content, now, now);
	}

	// 필요하다면 도메인 검증 메서드 등도 선언 가능
	public boolean isValid() {
		return star != null && star > 0
			&& catalogId != null && catalogId > 0
			&& content != null && !content.isBlank();
	}
}
