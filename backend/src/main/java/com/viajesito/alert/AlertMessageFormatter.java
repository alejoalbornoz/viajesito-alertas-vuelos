package com.viajesito.alert;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.viajesito.route.Route;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/** Arma el mensaje de Telegram (parse_mode=HTML) para una oferta. */
@Component
public class AlertMessageFormatter {

	private static final Locale ES_AR = Locale.forLanguageTag("es-AR");

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", ES_AR);

	public String format(Route route, AlertRequest alert) {
		StringBuilder msg = new StringBuilder();
		msg.append("✈️ <b>").append(alert.alertType().headline()).append("</b>\n");
		msg.append("<b>").append(route.getOrigin()).append(" → ").append(route.getDestination()).append("</b>")
			.append(route.getReturnDate() == null ? " · solo ida" : " · ida y vuelta").append('\n');

		msg.append("📅 ").append(DATE.format(route.getDepartureDate()));
		if (route.getReturnDate() != null) {
			msg.append(" → ").append(DATE.format(route.getReturnDate()));
		}
		msg.append('\n');

		msg.append("💰 <b>").append(money(alert.currency(), alert.price())).append("</b>");
		if (StringUtils.hasText(alert.airline())) {
			msg.append(" · ").append(HtmlUtils.htmlEscape(alert.airline()));
		}
		if (alert.stops() != null) {
			msg.append(" · ").append(stopsLabel(alert.stops()));
		}
		msg.append('\n');

		if (alert.savingsPct() != null && alert.averagePrice() != null) {
			boolean cheaper = alert.savingsPct().signum() >= 0;
			msg.append(cheaper ? "📉 " : "📈 ").append(percent(alert.savingsPct().abs()))
				.append(cheaper ? " por debajo" : " por encima").append(" del promedio (")
				.append(money(alert.currency(), alert.averagePrice())).append(")\n");
		}
		if (alert.has(AlertType.HISTORICAL_MIN) && alert.minPrice() != null) {
			msg.append("🏆 Nuevo mínimo histórico (el anterior era ")
				.append(money(alert.currency(), alert.minPrice())).append(")\n");
		}
		if (alert.has(AlertType.ANOMALY) && alert.zscore() != null) {
			msg.append("⚡ Precio anómalo: ").append(number(alert.zscore().abs()))
				.append(" desvíos estándar bajo la media\n");
		}
		if (alert.has(AlertType.TARGET_PRICE) && route.getTargetPriceMax() != null) {
			msg.append("🎯 Dentro de tu objetivo (≤ ").append(money(alert.currency(), route.getTargetPriceMax()))
				.append(")\n");
		}
		if (route.getTargetPriceMin() != null && alert.price().compareTo(route.getTargetPriceMin()) <= 0) {
			msg.append("💎 ¡Por debajo de tu precio soñado (").append(money(alert.currency(), route.getTargetPriceMin()))
				.append(")!\n");
		}
		if (alert.sampleSize() != null && alert.sampleSize() > 0) {
			msg.append("📊 ");
			if (alert.medianPrice() != null) {
				msg.append("Mediana ").append(money(alert.currency(), alert.medianPrice())).append(" · ");
			}
			msg.append(alert.sampleSize()).append(alert.sampleSize() == 1 ? " día" : " días").append(" de histórico");
			String trend = trendLabel(alert.trend());
			if (trend != null) {
				msg.append(" · ").append(trend);
			}
			msg.append('\n');
		}
		if (StringUtils.hasText(route.getNotes())) {
			msg.append("📝 ").append(HtmlUtils.htmlEscape(route.getNotes())).append('\n');
		}
		msg.append("\n<a href=\"").append(googleFlightsUrl(route)).append("\">Ver en Google Flights</a>");
		return msg.toString();
	}

	static String googleFlightsUrl(Route route) {
		String query = "Flights from " + route.getOrigin() + " to " + route.getDestination() + " on "
				+ route.getDepartureDate() + (route.getReturnDate() != null ? " through " + route.getReturnDate() : "");
		return "https://www.google.com/travel/flights?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
	}

	private static String money(String currency, BigDecimal amount) {
		NumberFormat format = NumberFormat.getNumberInstance(ES_AR);
		format.setMinimumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
		format.setMaximumFractionDigits(2);
		return currency + " " + format.format(amount);
	}

	private static String number(BigDecimal value) {
		NumberFormat format = NumberFormat.getNumberInstance(ES_AR);
		format.setMaximumFractionDigits(1);
		return format.format(value);
	}

	private static String percent(BigDecimal value) {
		return number(value) + "%";
	}

	private static String stopsLabel(int stops) {
		return switch (stops) {
			case 0 -> "directo";
			case 1 -> "1 escala";
			default -> stops + " escalas";
		};
	}

	private static String trendLabel(String trend) {
		if (trend == null) {
			return null;
		}
		return switch (trend) {
			case "DOWN" -> "tendencia ↘ bajando";
			case "UP" -> "tendencia ↗ subiendo";
			case "STABLE" -> "tendencia → estable";
			default -> null;
		};
	}

}
