package com.bervan.shopwebscraper;

import lombok.Getter;

@Getter
public class ProductScrapException extends Exception {
    private final ConfigProduct product;

    public ProductScrapException(String message, ConfigProduct product) {
        super(message);
        this.product = product;
    }
}
