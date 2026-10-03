package com.viajesito.alert;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

	Optional<AlertLog> findFirstByRouteIdAndStatusInOrderBySentAtDesc(Long routeId, Collection<AlertStatus> statuses);

	List<AlertLog> findAllByOrderBySentAtDesc(Limit limit);

	List<AlertLog> findByRouteIdOrderBySentAtDesc(Long routeId, Limit limit);

}
