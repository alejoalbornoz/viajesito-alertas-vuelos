package com.viajesito.route;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

	private final RouteService service;

	public RouteController(RouteService service) {
		this.service = service;
	}

	@GetMapping
	public List<RouteResponse> list(@RequestParam(required = false) Boolean active) {
		return service.list(active);
	}

	@GetMapping("/{id}")
	public RouteResponse get(@PathVariable Long id) {
		return service.get(id);
	}

	@PostMapping
	public ResponseEntity<RouteResponse> create(@Valid @RequestBody RouteRequest request) {
		RouteResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/routes/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public RouteResponse update(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
		return service.update(id, request);
	}

	@PostMapping("/{id}/pause")
	public RouteResponse pause(@PathVariable Long id) {
		return service.setActive(id, false);
	}

	@PostMapping("/{id}/resume")
	public RouteResponse resume(@PathVariable Long id) {
		return service.setActive(id, true);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
