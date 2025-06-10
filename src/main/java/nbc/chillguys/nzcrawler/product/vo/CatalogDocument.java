package nbc.chillguys.nzcrawler.product.vo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.entity.CatalogType;

@Document(indexName = "catalogs")
@Setting(settingPath = "/elastic/settings.json")
@Mapping(mappingPath = "/elastic/mappings.json")
public record CatalogDocument(

	@Id
	@Field(type = FieldType.Long)
	Long catalogId,

	@Field(type = FieldType.Text)
	String name,

	@Field(type = FieldType.Text)
	String description,

	@Field(type = FieldType.Keyword)
	String type,

	@Field(type = FieldType.Keyword)
	String manufacturer,

	@Field(type = FieldType.Keyword)
	String chipset,

	@Field(type = FieldType.Keyword)
	String formFactor,

	@Field(type = FieldType.Keyword)
	String socket
) {

	public static final Pattern pattern = Pattern.compile("\\(([^)]+)\\)");

	public static CatalogDocument from(Catalog catalog) {
		String[] splitName = catalog.getName().split(" ");
		String[] splitDescription = catalog.getDescription().split("/");

		String manufacturer = null;
		String chipset = null;
		String formFactor = null;
		String socket = null;
		if (catalog.getType() == CatalogType.CPU) {
			manufacturer = splitName[0].trim();
			Matcher matcher = pattern.matcher(splitDescription[0].trim());
			if (matcher.find()) {
				socket = matcher.group(1);
			}
		}
		if (catalog.getType() == CatalogType.GPU) {
			chipset = splitDescription[0].trim();
		}
		if (catalog.getType() == CatalogType.SSD) {
			formFactor = splitDescription[0].trim();
		}

		return new CatalogDocument(
			catalog.getId(),
			catalog.getName(),
			catalog.getDescription(),
			catalog.getType().name(),
			manufacturer,
			chipset,
			formFactor,
			socket
		);
	}

}
