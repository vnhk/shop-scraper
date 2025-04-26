package com.bervan.shopwebscraper.logging;


import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.bervan.logging.LogMessage;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class QueueAppender extends AppenderBase<ILoggingEvent> implements SmartLifecycle {

    private final RabbitTemplate rabbitTemplate;
    private final String applicationName;

    public QueueAppender(RabbitTemplate rabbitTemplate, @Value("${spring.application.name}") String applicationName) {
        this.rabbitTemplate = rabbitTemplate;
        this.applicationName = applicationName;
    }

    @Override
    public synchronized void doAppend(ILoggingEvent eventObject) {
        append(eventObject);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (rabbitTemplate == null || applicationName == null) {
            return;
        }
        LogMessage logMessage;
        if (eventObject.getCallerData() != null && eventObject.getCallerData().length > 0) {
            StackTraceElement callerData = eventObject.getCallerData()[0];
            logMessage = new LogMessage(
                    applicationName,
                    eventObject.getLevel().toString(),
                    eventObject.getFormattedMessage(),
                    LocalDateTime.now(),
                    callerData.getClassName(),
                    callerData.getMethodName(),
                    callerData.getLineNumber()
            );
        } else {
            logMessage = new LogMessage(
                    applicationName,
                    eventObject.getLevel().toString(),
                    eventObject.getFormattedMessage(),
                    LocalDateTime.now(),
                    "",
                    "",
                    -1
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
        super.start();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        AppenderDelegator<ILoggingEvent> delegate = (AppenderDelegator<ILoggingEvent>) rootLogger.getAppender("DELEGATOR");
        delegate.setDelegateAndReplayBuffer(this);
    }

    @Override
    public boolean isRunning() {
        return isStarted();
    }
}