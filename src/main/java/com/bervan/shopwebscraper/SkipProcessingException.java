package com.bervan.shopwebscraper;

public class SkipProcessingException extends RuntimeException {
    public SkipProcessingException(String msg) {
        super(msg);
    }
}
