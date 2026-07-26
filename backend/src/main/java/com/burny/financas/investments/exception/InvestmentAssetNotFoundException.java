package com.burny.financas.investments.exception;

public class InvestmentAssetNotFoundException extends RuntimeException {
    public InvestmentAssetNotFoundException(String message) {
        super(message);
    }
}
