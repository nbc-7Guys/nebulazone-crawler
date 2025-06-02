package nbc.chillguys.nzcrawler.product.dto;

public record ProductPageInfo(
	int categoryCode,
	int physicsCate1,
	int physicsCate2,
	int count
) {

	public static ProductPageInfo empty(int categoryCode) {
		return new ProductPageInfo(categoryCode, 0, 0, 0);
	}
}
