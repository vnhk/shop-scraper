package com.bervan.shopwebscraper;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConfigRoot {
    private String shopName;
    private String baseUrl;
    private List<ConfigProduct> products;
}
