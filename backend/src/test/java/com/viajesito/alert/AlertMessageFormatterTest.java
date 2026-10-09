package com.viajesito.alert;

import java.math.BigDecimal;
import java.util.List;

import com.viajesito.TestData;
import com.viajesito.route.Route;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertMessageFormatterTest {

	private final AlertMessageFormatter formatter = new AlertMessageFormatter();

	@Test
	void includesPriceSavingsAndSignals() {
		String msg = formatter.format(TestData.route(), TestData.alert("1030"));

		assertThat(msg)
			.contains("¡Mínimo histórico!")
			.contains("EZE → MAD")
			.contains("ida y vuelta")
			.contains("<b>USD 1.030</b>")
			.contains("Iberia · 1 escala")
			.contains("20,8% por debajo del promedio (USD 1.300)")
			.contains("el anterior era USD 1.150")
			.contains("Dentro de tu objetivo (≤ USD 1.100)")
			.contains("30 días de histórico")
			.contains("bajando")
			.contains("Google: precio típico (lo normal: USD 1.100 – USD 1.400)")
			.doesNotContain("Sale de")
			.contains("https://www.google.com/travel/flights?q=Flights+from+EZE+to+MAD");
	}

	@Test
	void multiAirportRouteShowsWhereTheDealDepartsFrom() {
		Route route = TestData.route();
		route.setOrigin("EZE,AEP");
		route.setDestination("GIG");
		AlertRequest alert = new AlertRequest(1L, null, new BigDecimal("310"), "USD", "JetSMART", 0,
				AlertType.GOOGLE_LOW, List.of(AlertType.GOOGLE_LOW), null, null, null, null, null, 0, null,
				"AEP", "GIG", "low", new BigDecimal("270"), new BigDecimal("380"));

		String msg = formatter.format(route, alert);

		assertThat(msg)
			.contains("¡Precio bajo según Google!")
			.contains("<b>EZE/AEP → GIG</b>")
			.contains("Sale de <b>AEP</b>")
			.doesNotContain("Llega a")
			.contains("Google: precio <b>BAJO</b> ✅ (lo normal: USD 270 – USD 380)")
			.contains("q=Flights+from+AEP+to+GIG");
	}

	@Test
	void flagsDreamPriceAndEscapesHtml() {
		Route route = TestData.route();
		route.setNotes("Vacaciones <familia> & amigos");
		AlertRequest alert = new AlertRequest(1L, null, new BigDecimal("850.50"), "USD", "Air <Europa>", 0,
				AlertType.TARGET_PRICE, List.of(AlertType.TARGET_PRICE), null, null, null, null, null, 0, null,
				null, null, null, null, null);

		String msg = formatter.format(route, alert);

		assertThat(msg)
			.contains("USD 850,50")
			.contains("Air &lt;Europa&gt; · directo")
			.contains("precio soñado")
			.contains("Vacaciones &lt;familia&gt; &amp; amigos")
			.doesNotContain("promedio")
			.doesNotContain("histórico")
			.doesNotContain("Google:");
	}

}
