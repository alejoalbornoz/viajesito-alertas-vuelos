package com.viajesito.route;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {

	List<Route> findAllByOrderByIdAsc();

	List<Route> findByActiveOrderByIdAsc(boolean active);

}
