package nbc.chillguys.nzcrawler.review.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import nbc.chillguys.nzcrawler.product.vo.CatalogDocument;
import nbc.chillguys.nzcrawler.review.vo.ReviewDocument;

public interface ReviewEsRepository extends ElasticsearchRepository<ReviewDocument, Long> {

}
