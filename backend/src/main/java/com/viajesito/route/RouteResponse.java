package com.viajesito.route;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RouteResponse(
		Long id,
		String origin,
		String destination,
		LocalDate departureDate,
		LocalDate returnDate,
		int adults,
		Integer maxStops,
		String currency,
		BigDecimal targetPriceMin,
		BigDecimal targetPriceMax,
		boolean active,
		String notes,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	static RouteResponse from(Route r) {
		return new RouteResponse(r.getId(), r.getOrigin(), r.getDestination(), r.getDepartureDate(),
				r.getReturnDate(), r.getAdults(), r.getMaxStops(), r.getCurrency(), r.getTargetPriceMin(), r.getTargetPriceMax(),
				r.isActive(), r.getNotes(), r.getCreatedAt(), r.getUpdatedAt());
	}

}
