package nbc.chillguys.nzcrawler.product.dto;

import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.entity.CatalogType;

public record ProductInfo(
	int categoryCode,
	int productCode,
	String name,
	String specs,
	String releaseDate
) {

	public Catalog toEntity() {
		return Catalog.builder()
			.productCode(productCode)
			.name(name)
			.description(specs)
			.type(CatalogType.fromCategoryCode(categoryCode))
			.build();
	}
}
