package nbc.chillguys.nzcrawler.product.service;

import static nbc.chillguys.nzcrawler.product.constants.ProductCategory.*;
import static nbc.chillguys.nzcrawler.product.constants.ProductCategory.GPU;
import static nbc.chillguys.nzcrawler.product.constants.ProductCategory.SSD_M2_NVME;
import static nbc.chillguys.nzcrawler.product.constants.ProductCategory.SSD_M2_SATA;
import static nbc.chillguys.nzcrawler.product.constants.ProductCategory.SSD_SATA_2_5_INCH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.crawler.ProductCrawler;
import nbc.chillguys.nzcrawler.product.dto.ProductInfo;
import nbc.chillguys.nzcrawler.product.dto.ProductPageInfo;
import nbc.chillguys.nzcrawler.product.entity.Catalog;
import nbc.chillguys.nzcrawler.product.repository.CatalogRepository;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductCrawlService {

	private final ProductCrawler productCrawler;
	private final CatalogRepository catalogRepository;

	@PostConstruct
	public void init() {
		execute();
	}

	public void execute() {
		long startTime = System.currentTimeMillis();

		List<Integer> categories = List.of(CPU, GPU, SSD_M2_NVME, SSD_SATA_2_5_INCH, SSD_M2_SATA);
		List<ProductPageInfo> productCounts = new ArrayList<>();
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<ProductPageInfo>> futures = new ArrayList<>();
			for (int categoryCode : categories) {
				futures.add(executor.submit(() -> productCrawler.getProductPageInfo(categoryCode)));
			}

			for (Future<ProductPageInfo> future : futures) {
				try {
					productCounts.add(future.get());
				} catch (Exception e) {
					log.error("Future에서 상품 페이지 정보 조회 중 예외 발생", e);
				}
			}
		}

		Map<Integer, List<ProductInfo>> productMap = new HashMap<>();
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<List<ProductInfo>>> futures = new ArrayList<>();
			for (ProductPageInfo countInfo : productCounts) {
				int loop = countInfo.count() / 150 + 1;
				for (int page = 1; page <= loop; page++) {
					int finalPage = page;

					futures.add(
						executor.submit(() ->
							productCrawler.crawlCategoryPage(
								countInfo.categoryCode(),
								countInfo.physicsCate1(),
								countInfo.physicsCate2(),
								finalPage
							)
						)
					);
				}
			}

			for (Future<List<ProductInfo>> future : futures) {
				try {
					List<ProductInfo> products = future.get();
					if (products.isEmpty()) {
						continue;
					}

					int categoryCode = products.getFirst().categoryCode();
					productMap
						.computeIfAbsent(categoryCode, k -> new ArrayList<>())
						.addAll(products);
				} catch (Exception e) {
					log.error("Future에서 상품 정보 수집 중 예외 발생", e);
				}
			}
		}

		for (List<ProductInfo> products : productMap.values()) {
			List<Catalog> catalogs = products.stream()
				.map(ProductInfo::toEntity)
				.toList();
			catalogRepository.saveAllAndFlush(catalogs);
		}

		log.info("총 걸린 시간 {}초", (System.currentTimeMillis() - startTime) / 1000);
	}
}
