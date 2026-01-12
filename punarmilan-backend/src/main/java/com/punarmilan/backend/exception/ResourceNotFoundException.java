package com.punarmilan.backend.exception;

public class ResourceNotFoundException extends RuntimeException {
	
	public ResourceNotFoundException(String massege) {
		super(massege);
	}

}
