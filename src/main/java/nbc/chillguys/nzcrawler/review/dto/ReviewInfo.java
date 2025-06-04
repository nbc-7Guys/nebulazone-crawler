package nbc.chillguys.nzcrawler.review.dto;

/**
 * Danawa 리뷰 정보를 표현하는 레코드 객체.
 * @param star 별점 (1~5)
 * @param content 리뷰 본문 내용
 */
public record ReviewInfo(int star, String content) {
}