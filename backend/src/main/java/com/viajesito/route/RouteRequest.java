package com.viajesito.route;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Alta o reemplazo completo de una ruta. Los campos opcionales toman valores por defecto. */
public record RouteRequest(
		@NotNull(message = "es obligatorio")
		@Pattern(regexp = "^[A-Za-z]{3}$", message = "debe ser un código IATA de 3 letras") String origin,
		@NotNull(message = "es obligatorio")
		@Pattern(regexp = "^[A-Za-z]{3}$", message = "debe ser un código IATA de 3 letras") String destination,
		@NotNull(message = "es obligatorio")
		@FutureOrPresent(message = "no puede estar en el pasado") LocalDate departureDate,
		LocalDate returnDate,
		@Min(value = 1, message = "mínimo 1") @Max(value = 9, message = "máximo 9") Integer adults,
		@Min(value = 0, message = "mínimo 0") @Max(value = 2, message = "máximo 2") Integer maxStops,
		@Pattern(regexp = "^[A-Za-z]{3}$", message = "debe ser un código ISO de moneda (USD, EUR, ARS...)") String currency,
		@Positive(message = "debe ser mayor a 0") BigDecimal targetPriceMin,
		@Positive(message = "debe ser mayor a 0") BigDecimal targetPriceMax,
		Boolean active,
		@Size(max = 255, message = "máximo 255 caracteres") String notes) {

	@AssertTrue(message = "returnDate no puede ser anterior a departureDate")
	public boolean isReturnAfterDeparture() {
		return returnDate == null || departureDate == null || !returnDate.isBefore(departureDate);
	}

	@AssertTrue(message = "origin y destination deben ser distintos")
	public boolean isDistinctAirports() {
		return origin == null || !origin.equalsIgnoreCase(destination);
	}

	@AssertTrue(message = "targetPriceMin no puede superar targetPriceMax")
	public boolean isTargetRangeValid() {
		return targetPriceMin == null || targetPriceMax == null || targetPriceMin.compareTo(targetPriceMax) <= 0;
	}

	void applyTo(Route route) {
		route.setOrigin(origin.toUpperCase());
		route.setDestination(destination.toUpperCase());
		route.setDepartureDate(departureDate);
		route.setReturnDate(returnDate);
		route.setAdults(adults != null ? adults : 1);
		route.setMaxStops(maxStops);
		route.setCurrency(currency != null ? currency.toUpperCase() : "USD");
		route.setTargetPriceMin(targetPriceMin);
		route.setTargetPriceMax(targetPriceMax);
		route.setActive(active == null || active);
		route.setNotes(notes);
	}

}
