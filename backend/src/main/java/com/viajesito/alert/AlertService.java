package com.viajesito.alert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.viajesito.common.NotFoundException;
import com.viajesito.route.Route;
import com.viajesito.route.RouteRepository;
import com.viajesito.telegram.TelegramClient;
import com.viajesito.telegram.TelegramException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

/**
 * Motor de notificaciones: recibe la oferta, aplica el anti-spam, envía a Telegram
 * y deja registro en alerts_log.
 */
@Service
public class AlertService {

	private static final Logger log = LoggerFactory.getLogger(AlertService.class);

	/** Estados que cuentan como "ya avisado" para el anti-spam. */
	private static final EnumSet<AlertStatus> DELIVERED = EnumSet.of(AlertStatus.SENT, AlertStatus.DRY_RUN);

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final RouteRepository routes;

	private final AlertLogRepository alerts;

	private final TelegramClient telegram;

	private final AlertMessageFormatter formatter;

	private final AlertProperties properties;

	private final Clock clock;

	public AlertService(RouteRepository routes, AlertLogRepository alerts, TelegramClient telegram,
			AlertMessageFormatter formatter, AlertProperties properties, Clock clock) {
		this.routes = routes;
		this.alerts = alerts;
		this.telegram = telegram;
		this.formatter = formatter;
		this.properties = properties;
		this.clock = clock;
	}

	public AlertResult process(AlertRequest request) {
		Route route = routes.findById(request.routeId()).orElseThrow(() -> NotFoundException.route(request.routeId()));
		if (!route.isActive()) {
			return AlertResult.suppressed("La ruta está pausada");
		}

		OffsetDateTime now = OffsetDateTime.now(clock);
		Optional<String> repeated = alerts.findFirstByRouteIdAndStatusInOrderBySentAtDesc(route.getId(), DELIVERED)
			.flatMap(last -> repetitionReason(last, request, now));
		if (repeated.isPresent()) {
			log.info("Alerta suprimida para ruta {}: {}", route.getId(), repeated.get());
			return AlertResult.suppressed(repeated.get());
		}

		AlertLog entry = newEntry(route, request, now);
		if (!telegram.isConfigured()) {
			entry.setStatus(AlertStatus.DRY_RUN);
			log.info("[DRY_RUN] Telegram sin configurar. Mensaje para ruta {}:\n{}", route.getId(), entry.getMessage());
		}
		else {
			try {
				telegram.sendMessage(entry.getMessage());
				entry.setStatus(AlertStatus.SENT);
				log.info("Alerta enviada a Telegram para ruta {} ({} {})", route.getId(), request.currency(),
						request.price());
			}
			catch (TelegramException ex) {
				entry.setStatus(AlertStatus.FAILED);
				entry.setError(truncate(ex.getMessage(), 500));
				log.warn("Falló el envío a Telegram para ruta {}: {}", route.getId(), ex.getMessage());
			}
		}
		alerts.save(entry);
		return new AlertResult(entry.getStatus(), entry.getId(), entry.getError());
	}

	public List<AlertLog> recent(Long routeId, int limit) {
		Limit max = Limit.of(Math.clamp(limit, 1, 500));
		return routeId == null ? alerts.findAllByOrderBySentAtDesc(max) : alerts.findByRouteIdOrderBySentAtDesc(routeId, max);
	}

	/**
	 * Una alerta se considera repetida si la anterior está dentro del cooldown y el
	 * precio nuevo no es al menos {@code minImprovementPct}% más bajo.
	 */
	Optional<String> repetitionReason(AlertLog last, AlertRequest request, OffsetDateTime now) {
		if (!last.getCurrency().equals(request.currency())) {
			return Optional.empty();
		}
		Duration elapsed = Duration.between(last.getSentAt(), now);
		if (elapsed.compareTo(properties.cooldown()) >= 0) {
			return Optional.empty();
		}
		BigDecimal factor = BigDecimal.ONE.subtract(properties.minImprovementPct().divide(HUNDRED, 6, RoundingMode.HALF_UP));
		BigDecimal threshold = last.getPrice().multiply(factor);
		if (request.price().compareTo(threshold) <= 0) {
			return Optional.empty();
		}
		return Optional.of("Ya se avisó %s %s hace %dh (cooldown %dh)".formatted(last.getCurrency(),
				last.getPrice().toPlainString(), elapsed.toHours(), properties.cooldownHours()));
	}

	private AlertLog newEntry(Route route, AlertRequest request, OffsetDateTime now) {
		AlertLog entry = new AlertLog();
		entry.setRouteId(route.getId());
		entry.setPriceHistoryId(request.priceHistoryId());
		entry.setAlertType(request.alertType());
		entry.setReasons(request.reasons().stream().map(Enum::name).collect(Collectors.joining(",")));
		entry.setPrice(request.price());
		entry.setCurrency(request.currency());
		entry.setReferencePrice(request.averagePrice());
		entry.setSavingsPct(request.savingsPct());
		entry.setMessage(formatter.format(route, request));
		entry.setSentAt(now);
		return entry;
	}

	private static String truncate(String text, int max) {
		return text == null || text.length() <= max ? text : text.substring(0, max);
	}

}
