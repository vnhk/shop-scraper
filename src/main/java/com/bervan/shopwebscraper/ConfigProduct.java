package com.bervan.shopwebscraper;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class ConfigProduct {
    private String name;
    private Set<String> categories;
    private String url;
    private Integer hour;
}
