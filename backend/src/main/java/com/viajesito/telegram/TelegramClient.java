package com.viajesito.telegram;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Cliente mínimo de la Bot API de Telegram: solo sendMessage en modo HTML. */
public class TelegramClient {

	private final RestClient restClient;

	private final TelegramProperties properties;

	/** URL armada a mano: como variable de template, el ':' del token se codificaría como %3A. */
	private final String sendMessageUrl;

	public TelegramClient(RestClient.Builder builder, TelegramProperties properties) {
		this.restClient = builder.build();
		this.properties = properties;
		this.sendMessageUrl = properties.apiUrl() + "/bot" + properties.botToken() + "/sendMessage";
	}

	public boolean isConfigured() {
		return properties.isConfigured();
	}

	public void sendMessage(String html) {
		if (!isConfigured()) {
			throw new TelegramException("Telegram no está configurado (TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID)");
		}
		Map<String, Object> body = Map.of(
				"chat_id", properties.chatId(),
				"text", html,
				"parse_mode", "HTML",
				"link_preview_options", Map.of("is_disabled", true));
		try {
			restClient.post()
				.uri(sendMessageUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.toBodilessEntity();
		}
		catch (RestClientResponseException ex) {
			// La respuesta de Telegram trae una "description" útil (chat not found, bot blocked...).
			throw new TelegramException("Telegram respondió " + ex.getStatusCode().value() + ": "
					+ ex.getResponseBodyAsString());
		}
		catch (RestClientException ex) {
			// No usamos ex.getMessage(): incluye la URL, que contiene el token del bot.
			throw new TelegramException("No se pudo contactar a Telegram (" + ex.getClass().getSimpleName() + ")");
		}
	}

}
