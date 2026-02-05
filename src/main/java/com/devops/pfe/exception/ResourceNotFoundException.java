package com.devops.pfe.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s avec l'id %d n'existe pas", resourceName, id));
    }
    
    public ResourceNotFoundException(String resourceName, String field, String value) {
        super(String.format("%s avec %s '%s' n'existe pas", resourceName, field, value));
    }
}
