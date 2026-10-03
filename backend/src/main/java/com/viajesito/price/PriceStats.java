package com.viajesito.price;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resumen simple del histórico de una ruta, para consultar desde la API.
 * El análisis "en serio" (precio diario, tendencia, anomalías) vive en el módulo Python.
 */
public record PriceStats(
		int samples,
		String currency,
		BigDecimal average,
		BigDecimal median,
		BigDecimal min,
		BigDecimal max,
		BigDecimal lastPrice,
		OffsetDateTime lastCheckedAt) {

	static PriceStats from(String currency, List<PricePoint> pointsOldestFirst) {
		if (pointsOldestFirst.isEmpty()) {
			return new PriceStats(0, currency, null, null, null, null, null, null);
		}
		List<BigDecimal> sorted = pointsOldestFirst.stream().map(PricePoint::getPrice).sorted().toList();
		int n = sorted.size();
		BigDecimal sum = sorted.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal median = n % 2 == 1 ? sorted.get(n / 2)
				: sorted.get(n / 2 - 1).add(sorted.get(n / 2)).divide(BigDecimal.TWO, 2, RoundingMode.HALF_UP);
		PricePoint last = pointsOldestFirst.getLast();
		return new PriceStats(n, currency, sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP), median,
				sorted.getFirst(), sorted.getLast(), last.getPrice(), last.getCheckedAt());
	}

}
