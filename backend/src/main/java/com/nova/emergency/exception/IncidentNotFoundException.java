package com.nova.emergency.exception;

public class IncidentNotFoundException extends ResourceNotFoundException {
    public IncidentNotFoundException(String message) {
        super(message);
    }
}
