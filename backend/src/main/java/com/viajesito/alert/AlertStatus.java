package com.viajesito.alert;

public enum AlertStatus {

	/** Enviada a Telegram. */
	SENT,
	/** Telegram sin configurar: se registró y logueó, pero no se envió. */
	DRY_RUN,
	/** Telegram devolvió error; no cuenta para el anti-spam, así que se reintentará. */
	FAILED,
	/** Descartada por anti-spam o ruta pausada. No se persiste. */
	SUPPRESSED

}
