package com.bervan.shopwebscraper;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConfigProduct {
    private String name;
    private List<String> categories;
    private String url;
}
