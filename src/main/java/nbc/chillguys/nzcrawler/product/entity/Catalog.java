package nbc.chillguys.nzcrawler.product.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "catalogs")
public class Catalog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "catalog_id")
	private Long id;

	@Column(nullable = false, unique = true)
	private Integer productCode;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 1000, columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CatalogType type;

	@Column(nullable = false)
	private LocalDateTime created_at;

	@Column(nullable = false)
	private LocalDateTime modified_at;

	@Builder
	public Catalog(Integer productCode, String name, String description, CatalogType type) {
		this.productCode = productCode;
		this.name = name;
		this.description = description;
		this.type = type;
		this.created_at = LocalDateTime.now();
		this.modified_at = LocalDateTime.now();
	}
}