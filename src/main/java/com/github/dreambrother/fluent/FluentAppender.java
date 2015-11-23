package com.github.dreambrother.fluent;

import org.fluentd.logger.FluentLogger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

public class FluentAppender extends AppenderBase<ILoggingEvent> {

    private FluentLogger fluentLogger;

    private String tag;
    private Integer port;
    private String host;

    @Override
    public void start() {
        super.start();
        if (tag == null || port == null || host == null) {
            throw new IllegalStateException("tag, host and port must be not null");
        }
        fluentLogger = FluentLogger.getLogger("", host, port);
    }

    @Override
    protected void append(ILoggingEvent event) {
        String message = event.getFormattedMessage();

        // add stack trace in case of error
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            String stackTrace = formatStackTrace(throwableProxy);
            message = String.format("%s%n%s",
                    message,
                    stackTrace);
        }

        Map<String, Object> logParams = new HashMap<>();
        logParams.put("log_level", event.getLevel());
        logParams.put("tid", event.getThreadName());
        logParams.put("class", event.getLoggerName());
        logParams.put("message", message);

        fluentLogger.log(tag, logParams);
    }

    private String formatStackTrace(IThrowableProxy throwableProxy) {
        String exceptionMessage = throwableProxy.getClassName();
        if (throwableProxy.getMessage() != null) {
            exceptionMessage += ": " + throwableProxy.getMessage();
        }
        String stackTrace = Arrays.asList(throwableProxy.getStackTraceElementProxyArray())
                .stream()
                .map(StackTraceElementProxy::toString)
                .collect(Collectors.joining("\n"));
        String stackTraceWithMessage = exceptionMessage + " " + stackTrace;
        if (throwableProxy.getCause() != null) {
            stackTraceWithMessage += "\n" + "Caused by: " + formatStackTrace(throwableProxy.getCause());
        }
        return stackTraceWithMessage;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
