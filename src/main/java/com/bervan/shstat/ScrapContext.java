package com.bervan.shstat;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class ScrapContext implements Serializable {
    private Date scrapDate;
    private String thread;
    private String contextId;
    private ConfigProduct product;
    private ConfigRoot root;

    public String getContextId() {
        if (Strings.isNullOrEmpty(contextId)) {
            contextId = UUID.randomUUID().toString();
        }
        return contextId;
    }
}
