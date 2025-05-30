package nbc.chillguys.nzcrawler.product.entity;

import static nbc.chillguys.nzcrawler.product.constants.ProductCategory.*;

import nbc.chillguys.nzcrawler.product.constants.ProductCategory;

public enum CatalogType {
	CPU, GPU, SSD;

	public static CatalogType fromCategoryCode(int categoryCode) {
		return switch (categoryCode) {
			case ProductCategory.CPU -> CatalogType.CPU;
			case ProductCategory.GPU -> CatalogType.GPU;
			case SSD_M2_NVME, SSD_SATA_2_5_INCH, SSD_M2_SATA -> CatalogType.SSD;
			default -> throw new IllegalArgumentException("Unexpected value: " + categoryCode);
		};
	}
}
