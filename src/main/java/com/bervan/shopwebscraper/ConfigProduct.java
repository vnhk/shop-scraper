package com.bervan.shopwebscraper;

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
    private ScrapTime scrapTime;
}
