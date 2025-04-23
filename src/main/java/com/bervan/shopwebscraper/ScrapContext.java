package com.bervan.shopwebscraper;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class ScrapContext implements Serializable {
    private Date scrapDate;
    private String thread;
    private ConfigProduct product;
    private ConfigRoot root;
}
