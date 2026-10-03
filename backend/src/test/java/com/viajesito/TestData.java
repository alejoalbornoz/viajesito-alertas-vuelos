package com.viajesito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.viajesito.alert.AlertRequest;
import com.viajesito.alert.AlertType;
import com.viajesito.route.Route;

public final class TestData {

	private TestData() {
	}

	public static Route route() {
		Route route = new Route();
		route.setId(1L);
		route.setOrigin("EZE");
		route.setDestination("MAD");
		route.setDepartureDate(LocalDate.of(2027, 1, 15));
		route.setReturnDate(LocalDate.of(2027, 1, 30));
		route.setCurrency("USD");
		route.setTargetPriceMin(new BigDecimal("900"));
		route.setTargetPriceMax(new BigDecimal("1100"));
		return route;
	}

	public static AlertRequest alert(String price) {
		return new AlertRequest(1L, 42L, new BigDecimal(price), "USD", "Iberia", 1, AlertType.HISTORICAL_MIN,
				List.of(AlertType.HISTORICAL_MIN, AlertType.BELOW_AVERAGE, AlertType.TARGET_PRICE),
				new BigDecimal("1300.00"), new BigDecimal("1280.00"), new BigDecimal("1150.00"), new BigDecimal("20.77"),
				new BigDecimal("-2.4"), 30, "DOWN");
	}

}
