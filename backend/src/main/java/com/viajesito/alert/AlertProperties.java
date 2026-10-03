package com.viajesito.alert;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Reglas anti-spam: dentro de {@code cooldownHours} desde la última alerta de una ruta,
 * solo se vuelve a avisar si el precio bajó al menos {@code minImprovementPct}%.
 */
@ConfigurationProperties("alerts")
public record AlertProperties(@DefaultValue("72") int cooldownHours, @DefaultValue("3") BigDecimal minImprovementPct) {

	public Duration cooldown() {
		return Duration.ofHours(cooldownHours);
	}

}
