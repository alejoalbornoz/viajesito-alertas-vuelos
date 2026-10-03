package com.viajesito.alert;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.viajesito.TestData;
import com.viajesito.common.NotFoundException;
import com.viajesito.route.Route;
import com.viajesito.route.RouteRepository;
import com.viajesito.telegram.TelegramClient;
import com.viajesito.telegram.TelegramException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AlertServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-02T11:00:00Z");

	private final RouteRepository routes = mock(RouteRepository.class);

	private final AlertLogRepository alerts = mock(AlertLogRepository.class);

	private final TelegramClient telegram = mock(TelegramClient.class);

	private AlertService service;

	private Route route;

	@BeforeEach
	void setUp() {
		service = new AlertService(routes, alerts, telegram, new AlertMessageFormatter(),
				new AlertProperties(72, new BigDecimal("3")), Clock.fixed(NOW, ZoneOffset.UTC));
		route = TestData.route();
		given(routes.findById(1L)).willReturn(Optional.of(route));
		given(alerts.findFirstByRouteIdAndStatusInOrderBySentAtDesc(anyLong(), any())).willReturn(Optional.empty());
		given(telegram.isConfigured()).willReturn(true);
	}

	@Test
	void sendsAndLogsFirstAlert() {
		AlertResult result = service.process(TestData.alert("1030"));

		assertThat(result.status()).isEqualTo(AlertStatus.SENT);
		verify(telegram).sendMessage(anyString());
		AlertLog saved = savedEntry();
		assertThat(saved.getStatus()).isEqualTo(AlertStatus.SENT);
		assertThat(saved.getReasons()).isEqualTo("HISTORICAL_MIN,BELOW_AVERAGE,TARGET_PRICE");
		assertThat(saved.getPriceHistoryId()).isEqualTo(42L);
		assertThat(saved.getSentAt()).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
	}

	@Test
	void suppressesSamePriceWithinCooldown() {
		givenLastAlert("1030", 24);

		AlertResult result = service.process(TestData.alert("1025"));

		assertThat(result.status()).isEqualTo(AlertStatus.SUPPRESSED);
		assertThat(result.detail()).contains("hace 24h");
		verify(telegram, never()).sendMessage(anyString());
		verify(alerts, never()).save(any());
	}

	@Test
	void sendsWithinCooldownWhenPriceDropsEnough() {
		givenLastAlert("1030", 24);

		// 3% menos que 1030 = 999.10
		assertThat(service.process(TestData.alert("999")).status()).isEqualTo(AlertStatus.SENT);
	}

	@Test
	void sendsAgainAfterCooldown() {
		givenLastAlert("1030", 72);

		assertThat(service.process(TestData.alert("1030")).status()).isEqualTo(AlertStatus.SENT);
	}

	@Test
	void dryRunWhenTelegramIsNotConfigured() {
		given(telegram.isConfigured()).willReturn(false);

		AlertResult result = service.process(TestData.alert("1030"));

		assertThat(result.status()).isEqualTo(AlertStatus.DRY_RUN);
		verify(telegram, never()).sendMessage(anyString());
		assertThat(savedEntry().getMessage()).contains("EZE → MAD");
	}

	@Test
	void recordsTelegramFailures() {
		willThrow(new TelegramException("Telegram respondió 400: chat not found")).given(telegram).sendMessage(anyString());

		AlertResult result = service.process(TestData.alert("1030"));

		assertThat(result.status()).isEqualTo(AlertStatus.FAILED);
		assertThat(result.detail()).contains("chat not found");
		assertThat(savedEntry().getError()).contains("chat not found");
	}

	@Test
	void ignoresPausedRoutes() {
		route.setActive(false);

		assertThat(service.process(TestData.alert("1030")).status()).isEqualTo(AlertStatus.SUPPRESSED);
		verify(alerts, never()).save(any());
	}

	@Test
	void unknownRouteIsNotFound() {
		given(routes.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.process(TestData.alert("1030"))).isInstanceOf(NotFoundException.class);
	}

	private void givenLastAlert(String price, int hoursAgo) {
		AlertLog last = new AlertLog();
		last.setPrice(new BigDecimal(price));
		last.setCurrency("USD");
		last.setStatus(AlertStatus.SENT);
		last.setSentAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(hoursAgo));
		given(alerts.findFirstByRouteIdAndStatusInOrderBySentAtDesc(anyLong(), any())).willReturn(Optional.of(last));
	}

	private AlertLog savedEntry() {
		ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
		verify(alerts).save(captor.capture());
		return captor.getValue();
	}

}
