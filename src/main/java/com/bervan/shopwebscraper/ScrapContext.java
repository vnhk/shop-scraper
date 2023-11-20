package com.bervan.shopwebscraper;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ScrapContext {
    private Date scrapDate;
    private String thread;
    private ConfigProduct product;
    private ConfigRoot root;
}
