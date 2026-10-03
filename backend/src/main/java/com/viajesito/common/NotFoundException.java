package com.viajesito.common;

public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

	public static NotFoundException route(Long id) {
		return new NotFoundException("No existe la ruta " + id);
	}

}
