package com.viajesito.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

@ConfigurationProperties("telegram")
public record TelegramProperties(String botToken, String chatId,
		@DefaultValue("https://api.telegram.org") String apiUrl) {

	public boolean isConfigured() {
		return StringUtils.hasText(botToken) && StringUtils.hasText(chatId);
	}

}
