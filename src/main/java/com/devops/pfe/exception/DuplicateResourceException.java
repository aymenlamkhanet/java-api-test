package com.devops.pfe.exception;

public class DuplicateResourceException extends BusinessException {
    
    public DuplicateResourceException(String resourceName, String field, String value) {
        super(String.format("%s avec %s '%s' existe déjà", resourceName, field, value), "DUPLICATE_RESOURCE");
    }
}
