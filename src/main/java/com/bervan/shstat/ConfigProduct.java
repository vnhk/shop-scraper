package com.bervan.shstat;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
public class ConfigProduct implements Serializable {
    private String name;
    private Set<String> categories;
    private String url;
    private Integer minPrice;
    private Integer maxPrice;
    private ScrapTime scrapTime;
}
