package com.bervan.shopwebscraper.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
public class LogMessage implements Serializable {
    private String applicationName;
    private String logLevel;
    private String message;
    private LocalDateTime timestamp;
    private String packageName;
    private String className;
    private String methodName;
    private String moduleName;
    private String processName;
    private String route;
    private Integer lineNumber;
    private String json;
}
