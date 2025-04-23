package com.bervan.shopwebscraper;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class ConfigRoot implements Serializable {
    private String shopName;
    private String baseUrl;
    private List<ConfigProduct> products;
}
