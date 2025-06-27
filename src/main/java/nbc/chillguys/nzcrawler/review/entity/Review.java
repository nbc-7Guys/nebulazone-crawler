package nbc.chillguys.nzcrawler.review.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nbc.chillguys.nzcrawler.review.dto.ReviewInfo;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reviewId;

	@Column(nullable = false)
	private Integer star;

	@Column(nullable = false)
	private Long catalogId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime modifiedAt;

	@Builder
	public Review(Integer star, Long catalogId, String content) {
		this.star = star;
		this.catalogId = catalogId;
		this.content = content;
	}

	public static Review from(ReviewInfo info) {
		return Review.builder()
			.star(info.star())
			.catalogId(info.catalogId())
			.content(info.content())
			.build();
	}

	@PrePersist
	public void onPrePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.modifiedAt = now;
	}

	@PreUpdate
	public void onPreUpdate() {
		this.modifiedAt = LocalDateTime.now();
	}
}
