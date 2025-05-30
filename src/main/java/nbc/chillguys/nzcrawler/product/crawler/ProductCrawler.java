package nbc.chillguys.nzcrawler.product.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import nbc.chillguys.nzcrawler.product.dto.ProductInfo;
import nbc.chillguys.nzcrawler.product.dto.ProductPageInfo;
import nbc.chillguys.nzcrawler.product.util.DateUtil;

@Slf4j
@Component
public class ProductCrawler {

	public static final int PRODUCT_COUNT_PER_PAGE = 150;

	public ProductPageInfo getProductPageInfo(int categoryCode) {
		try {
			String url = "https://prod.danawa.com/list/?cate=" + categoryCode;
			Document doc = Jsoup.connect(url)
				.userAgent("Mozilla/5.0")
				.get();

			Element physicsCategoryElement = doc.selectFirst("input#firstProductPhysicsCategoryCode");
			if (physicsCategoryElement == null) {
				log.warn("물리 카테고리 정보를 찾을 수 없습니다 - categoryCode: {}", categoryCode);
				return new ProductPageInfo(categoryCode, 0, 0, 0);
			}

			String physicsCategory = physicsCategoryElement.attr("value");
			String[] parts = physicsCategory.split("\\|");
			if (parts.length < 2) {
				log.warn("물리 카테고리 형식이 올바르지 않습니다 - categoryCode: {}, rawValue: {}", categoryCode, physicsCategory);
				return new ProductPageInfo(categoryCode, 0, 0, 0);
			}

			int physicsCate1 = Integer.parseInt(parts[0]);
			int physicsCate2 = Integer.parseInt(parts[1]);

			Element countElement = doc.selectFirst("strong.list_num");
			if (countElement == null) {
				log.warn("상품 개수 정보를 찾을 수 없습니다 - categoryCode: {}", categoryCode);
				return new ProductPageInfo(categoryCode, physicsCate1, physicsCate2, 0);
			}

			String countText = countElement.text().replaceAll("[^\\d]", "");
			if (!countText.matches("\\d+")) {
				log.warn("상품 개수 형식이 올바르지 않습니다 - categoryCode: {}, rawText: {}", categoryCode, countText);
				return new ProductPageInfo(categoryCode, physicsCate1, physicsCate2, 0);
			}

			int productCount = Integer.parseInt(countText);
			return new ProductPageInfo(categoryCode, physicsCate1, physicsCate2, productCount);

		} catch (Exception e) {
			log.error("상품 페이지 정보 수집 중 예외 발생 - categoryCode: {}", categoryCode, e);
			return ProductPageInfo.empty(categoryCode);
		}
	}

	public List<ProductInfo> crawlCategoryPage(int categoryCode, int physicsCate1, int physicsCate2, int page) {
		List<ProductInfo> products = new ArrayList<>();
		String url = "https://prod.danawa.com/list/ajax/getProductList.ajax.php";
		String referer = "https://prod.danawa.com/list/?cate=" + categoryCode;

		boolean isSubCategory = categoryCode > 999999;
		String subCategoryCode = String.valueOf(isSubCategory ? categoryCode % 100000 : categoryCode % 1000);

		try {
			Document doc = Jsoup.connect(url)
				.header("User-Agent", "Mozilla/5.0")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Referer", referer)
				.data("page", String.valueOf(page))
				.data("listCategoryCode", subCategoryCode)
				.data("categoryCode", subCategoryCode)
				.data("priceUnit", "0")
				.data("sortMethod", "NEW")
				.data("listCount", String.valueOf(PRODUCT_COUNT_PER_PAGE))
				.data("viewMethod", "LIST")
				.data("physicsCate1", String.valueOf(physicsCate1))
				.data("physicsCate2", String.valueOf(physicsCate2))
				.data("sProductListApi", "search")
				.post();

			Elements items = doc.select("li.prod_item");
			int itemIdx = 0;

			for (Element item : items) {
				try {
					Element nameElement = item.selectFirst("div.prod_info > p.prod_name > a");
					if (nameElement == null) {
						log.error("상품명 추출 실패 - categoryCode: {}, page: {}, itemIdx: {}", categoryCode, page, itemIdx);
						continue;
					}

					String name = nameElement.text().trim();
					String link = nameElement.absUrl("href");
					if (name.contains("중고")) {
						continue;
					}

					Matcher matcher = Pattern.compile("pcode=(\\d+)").matcher(link);
					int productCode = matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
					if (productCode == 0) {
						log.warn("상품 코드 추출 실패 - 링크: {}, categoryCode: {}, page: {}, itemIdx: {}", link, categoryCode, page, itemIdx);
					}

					Elements specElement = item.select("div.spec-box > div.spec_list");
					String specs = specElement.isEmpty()
						? ""
						: (specElement.size() > 1 ? specElement.get(1).text().trim() : specElement.get(0).text().trim());

					Element dateElement = item.selectFirst("dl.mt_date");
					if (dateElement == null) {
						log.warn("출시일 정보 누락 - categoryCode: {}, page: {}, itemIdx: {}", categoryCode, page, itemIdx);
						continue;
					}

					String[] dateParts = dateElement.text().trim().split("\\s+");
					if (dateParts.length < 2) {
						log.warn("출시일 텍스트 형식 오류 - 텍스트: '{}', categoryCode: {}, page: {}, itemIdx: {}", dateElement.text(), categoryCode, page, itemIdx);
						continue;
					}

					String releaseRaw = dateParts[1];
					String[] parts = releaseRaw.split("\\.");
					if (parts.length < 2) {
						log.warn("출시일 상세 형식 오류 - 값: '{}', categoryCode: {}, page: {}, itemIdx: {}", releaseRaw, categoryCode, page, itemIdx);
						continue;
					}

					String year = parts[0];
					int month = Integer.parseInt(parts[1]);
					String releaseDate = DateUtil.getQuarter(month) + " " + year;

					products.add(new ProductInfo(categoryCode, productCode, name, specs, releaseDate));
				} catch (Exception e) {
					log.error("상품 상세 정보 파싱 중 오류 발생 - categoryCode: {}, page: {}, itemIdx: {}", categoryCode, page, itemIdx, e);
				} finally {
					itemIdx++;
				}
			}
		} catch (Exception e) {
			log.error("카테고리 페이지 크롤링 실패 - categoryCode: {}, physicsCate1: {}, physicsCate2: {}, page: {}", categoryCode, physicsCate1, physicsCate2, page, e);
		}

		return products;
	}
}
