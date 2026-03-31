package br.com.bertan.finsync.exception;

public class InvalidReconciliationStateException extends RuntimeException {

    public InvalidReconciliationStateException(String message) {
        super(message);
    }
}