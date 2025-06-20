package nbc.chillguys.nzcrawler.product.vo;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.entity.CatalogType;
import nbc.chillguys.nzcrawler.product.util.CatalogExtractUtil;

@Document(indexName = "catalogs")
@Setting(settingPath = "/elastic/settings.json")
@Mapping(mappingPath = "/elastic/mappings.json")
public record CatalogDocument(

	@Id
	@Field(type = FieldType.Long)
	Long catalogId,

	@Field(type = FieldType.Text, analyzer = "korean_english")
	String name,

	@Field(type = FieldType.Text, analyzer = "korean_english")
	String description,

	@Field(type = FieldType.Keyword)
	String type,

	@Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
	LocalDateTime createdAt,

	@Field(type = FieldType.Keyword)
	String manufacturer,

	@Field(type = FieldType.Keyword)
	String chipset,

	@Field(type = FieldType.Keyword)
	String formFactor,

	@Field(type = FieldType.Keyword)
	String socket
) {

	public static CatalogDocument from(Catalog catalog) {
		String manufacturer = null;
		String chipset = null;
		String formFactor = null;
		String socket = null;

		if (catalog.getType() == CatalogType.CPU) {
			manufacturer = CatalogExtractUtil.extractFirstWord(catalog.getName());
			socket = CatalogExtractUtil.extractSocket(catalog.getDescription());
		} else if (catalog.getType() == CatalogType.GPU) {
			chipset = CatalogExtractUtil.extractFirstDescriptionPart(catalog.getDescription());
		} else if (catalog.getType() == CatalogType.SSD) {
			formFactor = CatalogExtractUtil.extractFirstDescriptionPart(catalog.getDescription());
		}

		return new CatalogDocument(
			catalog.getId(),
			catalog.getName(),
			catalog.getDescription(),
			catalog.getType().name(),
			catalog.getCreatedAt(),
			manufacturer,
			chipset,
			formFactor,
			socket
		);
	}
}
