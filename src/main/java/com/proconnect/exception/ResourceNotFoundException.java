package com.proconnect.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public static ResourceNotFoundException professionalNotFound(Long id) {
        return new ResourceNotFoundException("Professional not found with id: " + id);
    }
}
