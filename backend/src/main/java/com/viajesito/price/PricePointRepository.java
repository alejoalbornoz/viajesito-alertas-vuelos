package com.viajesito.price;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricePointRepository extends JpaRepository<PricePoint, Long> {

	List<PricePoint> findByRouteIdOrderByCheckedAtDesc(Long routeId, Limit limit);

	List<PricePoint> findByRouteIdAndCurrencyOrderByCheckedAtAsc(Long routeId, String currency);

}
