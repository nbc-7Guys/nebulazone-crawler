package nbc.chillguys.nzcrawler.product.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import nbc.chillguys.nzcrawler.product.vo.CatalogDocument;

public interface CatalogEsRepository extends ElasticsearchRepository<CatalogDocument, Long> {

}
