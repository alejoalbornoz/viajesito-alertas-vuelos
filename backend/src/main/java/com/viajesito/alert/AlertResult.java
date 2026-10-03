package com.viajesito.alert;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertResult(AlertStatus status, Long alertId, String detail) {

	static AlertResult suppressed(String detail) {
		return new AlertResult(AlertStatus.SUPPRESSED, null, detail);
	}

}
