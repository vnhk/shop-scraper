package com.bervan.shopwebscraper;


import org.apache.logging.log4j.message.StringFormattedMessage;
import org.slf4j.Logger;

public class LogUtils {
    public static void info(Logger log, ScrapContext scrapContext, String messageFormat, Object... params) {
        StringFormattedMessage stringFormattedMessage = new StringFormattedMessage(messageFormat, params);
        log.info("{} : {} : {} - {} - {}",
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
        log.debug("{} : {} : {} - {} - {}",
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                stringFormattedMessage.getFormattedMessage());
    }

    public static void info(Logger log, ScrapContext scrapContext, String message) {
        log.info("{} : {} : {} - {} - {}",
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                message);
    }

    public static void error(Logger log, ScrapContext scrapContext, String message, Exception e) {
        log.error("{} : {} : {} - {} - {}\n{}",
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                message,
                e.getMessage(),
                e);
    }

    public static void debug(Logger log, ScrapContext scrapContext, String message) {
        log.debug("{} : {} : {} - {} - {}",
                scrapContext.getRoot().getShopName(),
                getProductName(scrapContext),
                scrapContext.getScrapDate(),
                scrapContext.getThread(),
                message);
    }
}
