package com.viajesito.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protección simple para uso personal: si {@code app.api-key} está definido, toda
 * llamada a /api/** debe incluir el header {@code X-Api-Key} con ese valor.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Api-Key";

	private final byte[] expected;

	public ApiKeyFilter(@Value("${app.api-key:}") String apiKey) {
		this.expected = apiKey.isBlank() ? null : apiKey.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return expected == null || !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String provided = request.getHeader(HEADER);
		if (provided != null && MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8))) {
			chain.doFilter(request, response);
			return;
		}
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter()
			.write("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Falta o es inválido el header X-Api-Key\"}");
	}

}
