package com.viajesito.route;

import java.util.List;

import com.viajesito.common.NotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RouteService {

	private final RouteRepository routes;

	public RouteService(RouteRepository routes) {
		this.routes = routes;
	}

	@Transactional(readOnly = true)
	public List<RouteResponse> list(Boolean active) {
		List<Route> result = active == null ? routes.findAllByOrderByIdAsc() : routes.findByActiveOrderByIdAsc(active);
		return result.stream().map(RouteResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public RouteResponse get(Long id) {
		return RouteResponse.from(find(id));
	}

	public RouteResponse create(RouteRequest request) {
		Route route = new Route();
		request.applyTo(route);
		return RouteResponse.from(routes.save(route));
	}

	public RouteResponse update(Long id, RouteRequest request) {
		Route route = find(id);
		request.applyTo(route);
		return RouteResponse.from(routes.saveAndFlush(route));
	}

	public RouteResponse setActive(Long id, boolean active) {
		Route route = find(id);
		route.setActive(active);
		return RouteResponse.from(routes.saveAndFlush(route));
	}

	public void delete(Long id) {
		routes.delete(find(id));
	}

	Route find(Long id) {
		return routes.findById(id).orElseThrow(() -> NotFoundException.route(id));
	}

}
