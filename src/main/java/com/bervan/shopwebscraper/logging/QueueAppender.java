package com.bervan.shopwebscraper.logging;


import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class QueueAppender extends ConsoleAppender<ILoggingEvent> implements SmartLifecycle {

    private final RabbitTemplate rabbitTemplate;
    private final String applicationName;
    private final Boolean enabled;

    public QueueAppender(RabbitTemplate rabbitTemplate, @Value("${spring.application.name}") String applicationName, @Value("${queue-appender-enabled}") Boolean enabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.applicationName = applicationName;
        this.enabled = enabled;
    }

    @Override
    public synchronized void doAppend(ILoggingEvent eventObject) {
        append(eventObject);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (rabbitTemplate == null || applicationName == null || !enabled) {
            return;
        }

        byte[] encodedMessage = encoder.encode(eventObject);
        String decodedString = new String(encodedMessage, StandardCharsets.UTF_8);

        LogMessage logMessage;
        if (eventObject.getCallerData() != null && eventObject.getCallerData().length > 0) {
            StackTraceElement callerData = eventObject.getCallerData()[0];
            logMessage = new LogMessage(
                    applicationName,
                    eventObject.getLevel().toString(),
                    decodedString,
                    LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(eventObject.getTimeStamp()),
                            ZoneId.systemDefault()
                    ),
                    null,
                    callerData.getClassName(),
                    callerData.getMethodName(),
                    null,
                    null,
                    null,
                    callerData.getLineNumber(),
                    null
            );
        } else {
            logMessage = new LogMessage(
                    applicationName,
                    eventObject.getLevel().toString(),
                    decodedString,
                    LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(eventObject.getTimeStamp()),
                            ZoneId.systemDefault()
                    ),
                    null,
                    "",
                    "",
                    null,
                    null,
                    null,
                    -1,
                    null
            );
        }

        try {
            rabbitTemplate.convertAndSend("LOGS_DIRECT_EXCHANGE", "LOGS_ROUTING_KEY", logMessage);
        } catch (Exception e) {
            addError("Failed to send log to RabbitMQ", e);
        }
    }

    @Override
    public void start() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n");
        encoder.start();

        this.encoder = encoder;
        super.start();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        AppenderDelegator<ILoggingEvent> delegate = (AppenderDelegator<ILoggingEvent>) rootLogger.getAppender("DELEGATOR");
        delegate.setDelegateAndReplayBuffer(this);
    }

    @Override
    public boolean isRunning() {
        return isStarted();
    }
}