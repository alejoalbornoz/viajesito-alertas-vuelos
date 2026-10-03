package com.viajesito.telegram;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

	private final TelegramClient telegram;

	public TelegramController(TelegramClient telegram) {
		this.telegram = telegram;
	}

	/** Manda un mensaje de prueba para verificar token y chat_id. */
	@PostMapping("/test")
	public Map<String, String> test() {
		if (!telegram.isConfigured()) {
			return Map.of("status", "NOT_CONFIGURED",
					"detail", "Definí TELEGRAM_BOT_TOKEN y TELEGRAM_CHAT_ID en el .env");
		}
		try {
			telegram.sendMessage("✅ <b>Viajesito</b> está conectado. Acá vas a recibir las ofertas de vuelos.");
			return Map.of("status", "SENT");
		}
		catch (TelegramException ex) {
			return Map.of("status", "FAILED", "detail", ex.getMessage());
		}
	}

}
