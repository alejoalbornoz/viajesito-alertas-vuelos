package com.viajesito.price;

import java.util.List;

import com.viajesito.common.NotFoundException;
import com.viajesito.route.Route;
import com.viajesito.route.RouteRepository;

import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes/{routeId}")
public class PriceController {

	private final RouteRepository routes;

	private final PricePointRepository prices;

	public PriceController(RouteRepository routes, PricePointRepository prices) {
		this.routes = routes;
		this.prices = prices;
	}

	/** Últimos sondeos, del más reciente al más antiguo. */
	@GetMapping("/prices")
	public List<PricePoint> recent(@PathVariable Long routeId, @RequestParam(defaultValue = "30") int limit) {
		requireRoute(routeId);
		return prices.findByRouteIdOrderByCheckedAtDesc(routeId, Limit.of(Math.clamp(limit, 1, 1000)));
	}

	@GetMapping("/stats")
	public PriceStats stats(@PathVariable Long routeId) {
		Route route = requireRoute(routeId);
		return PriceStats.from(route.getCurrency(),
				prices.findByRouteIdAndCurrencyOrderByCheckedAtAsc(routeId, route.getCurrency()));
	}

	private Route requireRoute(Long routeId) {
		return routes.findById(routeId).orElseThrow(() -> NotFoundException.route(routeId));
	}

}
