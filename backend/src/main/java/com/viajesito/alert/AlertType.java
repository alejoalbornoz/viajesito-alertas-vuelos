package com.viajesito.alert;

/** Señales que puede detectar el analizador Python, de mayor a menor prioridad. */
public enum AlertType {

	HISTORICAL_MIN("¡Mínimo histórico!"),
	ANOMALY("¡Precio anómalo!"),
	BELOW_AVERAGE("¡Oferta detectada!"),
	TARGET_PRICE("Precio dentro de tu objetivo");

	private final String headline;

	AlertType(String headline) {
		this.headline = headline;
	}

	public String headline() {
		return headline;
	}

}
