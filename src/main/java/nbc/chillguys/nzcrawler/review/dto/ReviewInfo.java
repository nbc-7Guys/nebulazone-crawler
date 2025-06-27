package nbc.chillguys.nzcrawler.review.dto;

import java.time.LocalDateTime;

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

}
