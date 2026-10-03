package com.viajesito.price;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/** Un sondeo de precio. Lo escribe el módulo Python; acá solo se lee. */
@Entity
@Immutable
@Table(name = "price_history")
public class PricePoint {

	@Id
	private Long id;

	@Column(nullable = false)
	private Long routeId;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, length = 3)
	private String currency;

	private String airline;

	private Integer stops;

	private Integer durationMinutes;

	@Column(nullable = false)
	private String source;

	@Column(nullable = false)
	private OffsetDateTime checkedAt;

	protected PricePoint() {
	}

	public Long getId() {
		return id;
	}

	public Long getRouteId() {
		return routeId;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public String getCurrency() {
		return currency;
	}

	public String getAirline() {
		return airline;
	}

	public Integer getStops() {
		return stops;
	}

	public Integer getDurationMinutes() {
		return durationMinutes;
	}

	public String getSource() {
		return source;
	}

	public OffsetDateTime getCheckedAt() {
		return checkedAt;
	}

}
