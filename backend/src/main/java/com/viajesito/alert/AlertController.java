package com.viajesito.alert;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

	private final AlertService service;

	public AlertController(AlertService service) {
		this.service = service;
	}

	/**
	 * Lo invoca el analizador Python al detectar una oferta. Siempre responde 200 con
	 * el resultado (SENT, DRY_RUN, FAILED o SUPPRESSED) para que el cliente no reintente
	 * decisiones que ya se tomaron.
	 */
	@PostMapping
	public AlertResult receive(@Valid @RequestBody AlertRequest request) {
		return service.process(request);
	}

	@GetMapping
	public List<AlertLog> recent(@RequestParam(required = false) Long routeId,
			@RequestParam(defaultValue = "50") int limit) {
		return service.recent(routeId, limit);
	}

}
