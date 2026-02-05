package com.devops.pfe.exception;

public class InsufficientStockException extends BusinessException {
    
    public InsufficientStockException(String productName, Integer available, Integer requested) {
        super(String.format("Stock insuffisant pour '%s'. Disponible: %d, Demandé: %d", 
            productName, available, requested), "INSUFFICIENT_STOCK");
    }
}
