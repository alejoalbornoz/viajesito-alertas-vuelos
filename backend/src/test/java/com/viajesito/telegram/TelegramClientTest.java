package com.viajesito.telegram;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramClientTest {

	private static final TelegramProperties PROPS = new TelegramProperties("123:ABC", "999", "https://api.telegram.org");

	@Test
	void postsHtmlMessageToChat() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.telegram.org/bot123:ABC/sendMessage"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(jsonPath("$.chat_id").value("999"))
			.andExpect(jsonPath("$.parse_mode").value("HTML"))
			.andExpect(jsonPath("$.text").value("<b>hola</b>"))
			.andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

		new TelegramClient(builder, PROPS).sendMessage("<b>hola</b>");

		server.verify();
	}

	@Test
	void surfacesTelegramErrorDescription() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://api.telegram.org/bot123:ABC/sendMessage"))
			.andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
				.body("{\"ok\":false,\"description\":\"Bad Request: chat not found\"}"));

		TelegramClient client = new TelegramClient(builder, PROPS);

		assertThatThrownBy(() -> client.sendMessage("hola")).isInstanceOf(TelegramException.class)
			.hasMessageContaining("chat not found")
			.hasMessageNotContaining("123:ABC");
	}

	@Test
	void notConfiguredWithoutTokenOrChat() {
		TelegramClient client = new TelegramClient(RestClient.builder(),
				new TelegramProperties("", "999", "https://api.telegram.org"));

		assertThat(client.isConfigured()).isFalse();
		assertThatThrownBy(() -> client.sendMessage("hola")).isInstanceOf(TelegramException.class);
	}

}
