package com.bervan.shopwebscraper;


import com.bervan.shstat.ScrapContext;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.slf4j.Logger;

public class LogUtils {
    public static void info(Logger log, ScrapContext scrapContext, String messageFormat, Object... params) {
        StringFormattedMessage stringFormattedMessage = new StringFormattedMessage(messageFormat, params);
        log.info("ID={} : {} : {} : {} - {} - {}",
                scrapContext.getContextId(),
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                stringFormattedMessage.getFormattedMessage());
    }

    public static void warn(Logger log, ScrapContext scrapContext, String messageFormat, Object... params) {
        StringFormattedMessage stringFormattedMessage = new StringFormattedMessage(messageFormat, params);
        log.warn("ID={} : {} : {} : {} - {} - {}",
                scrapContext.getContextId(),
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                stringFormattedMessage.getFormattedMessage());
    }

    private static String getProductName(ScrapContext scrapContext) {
        if (scrapContext.getProduct() == null || scrapContext.getProduct().getName() == null || scrapContext.getProduct().getName().isBlank()) {
            return "_";
        }
        return scrapContext.getProduct().getName();
    }

    public static void debug(Logger log, ScrapContext scrapContext, String messageFormat, Object... params) {
        StringFormattedMessage stringFormattedMessage = new StringFormattedMessage(messageFormat, params);
        log.debug("ID={} : {} : {} : {} - {} - {}",
                scrapContext.getContextId(),
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                stringFormattedMessage.getFormattedMessage());
    }

    public static void info(Logger log, ScrapContext scrapContext, String message) {
        log.info("ID={} : {} : {} : {} - {} - {}",
                scrapContext.getContextId(),
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                message);
    }

    public static void error(Logger log, ScrapContext scrapContext, String message, Exception e) {
        log.error("ID={} : {} : {} : {} - {} - {}\n{}",
                scrapContext.getContextId(),
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                message,
                e.getMessage(),
                e);
    }

    public static void error(Logger log, ScrapContext scrapContext, Exception e, String messageFormat, Object... params) {
        StringFormattedMessage stringFormattedMessage = new StringFormattedMessage(messageFormat, params);
        log.error("ID={} : {} : {} : {} - {} - {}\n{}",
                scrapContext.getContextId(),
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                stringFormattedMessage,
                e.getMessage(),
                e);
    }

    public static void error(Logger log, ScrapContext scrapContext, String messageFormat, Object... params) {
        StringFormattedMessage stringFormattedMessage = new StringFormattedMessage(messageFormat, params);
        log.error("ID={} : {} : {} : {} - {} - {}",
                scrapContext.getContextId(),
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                stringFormattedMessage);
    }

    public static void debug(Logger log, ScrapContext scrapContext, String message) {
        log.debug("ID={} : {} : {} : {} - {} - {}",
                scrapContext.getContextId(),
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                message);
    }
}
