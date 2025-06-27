package nbc.chillguys.nzcrawler.review.vo;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import nbc.chillguys.nzcrawler.review.entity.Review;

@Document(indexName = "reviews")
public record ReviewDocument(

	@Id
	@Field(type = FieldType.Long)
	Long reviewId,

	@Field(type = FieldType.Long)
	Long catalogId,

	@Field(type = FieldType.Integer)
	Integer star,

	@Field(type = FieldType.Text, analyzer = "korean_english")
	String content,

	@Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
	LocalDateTime createdAt

) {
	public static ReviewDocument from(Review review) {
		return new ReviewDocument(
			review.getReviewId(),
			review.getCatalogId(),
			review.getStar(),
			review.getContent(),
			review.getCreatedAt()
		);
	}
}
