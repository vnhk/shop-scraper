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
    private String className;
    private String methodName;
    private int lineNumber;
}
