package com.viajesito.alert;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Oferta detectada por el analizador Python (POST /api/alerts). */
public record AlertRequest(
		@NotNull Long routeId,
		Long priceHistoryId,
		@NotNull @Positive BigDecimal price,
		@NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency,
		String airline,
		Integer stops,
		@NotNull AlertType alertType,
		@NotEmpty List<AlertType> reasons,
		BigDecimal averagePrice,
		BigDecimal medianPrice,
		BigDecimal minPrice,
		BigDecimal savingsPct,
		BigDecimal zscore,
		Integer sampleSize,
		String trend) {

	boolean has(AlertType type) {
		return reasons.contains(type);
	}

}
