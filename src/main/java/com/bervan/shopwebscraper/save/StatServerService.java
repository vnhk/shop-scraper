package com.bervan.shopwebscraper.save;

import com.bervan.shopwebscraper.Offer;
import com.bervan.shstat.queue.QueueMessage;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class StatServerService {
//    @Value("${stat-server.host:http://localhost}")
//    private String STAT_SERVER_HOST = "http://localhost";
//
//    @Value("${stat-server.port:8080}")
//    private String STAT_SERVER_PORT = "8080";

    @Value("${stat-server.apiKey}")
    private String apiKey;

    @Autowired
    private AmqpTemplate amqpTemplate;

//    @Value("${send-to-queue:false}")
//    private Boolean sendToQueue = false;

//    @Autowired
//    private RestTemplate restTemplate;

    public Set<String> refreshViews() throws SavingOffersToDBException {
        Set<String> res = new HashSet<>();
        try {
            HashMap<String, String> data = new HashMap<>();
            data.put("viewName", "HISTORICAL_LOW_PRICES");
            sendProductMessage(new QueueMessage("RefreshViewQueueParam", data, apiKey));
            data.put("viewName", "LOWER_THAN_HISTORICAL_LOW_PRICES");
            sendProductMessage(new QueueMessage("RefreshViewQueueParam", data, apiKey));
            data.put("viewName", "LOWER_THAN_AVG_FOR_LAST_MONTH");
            sendProductMessage(new QueueMessage("RefreshViewQueueParam", data, apiKey));
            data.put("viewName", "LOWER_THAN_AVG_FOR_LAST_X_MONTHS");
            sendProductMessage(new QueueMessage("RefreshViewQueueParam", data, apiKey));
        } catch (Exception e) {
            throw new SavingOffersToDBException("Views could not be refreshed!", e);
        }
        return res;
    }

//    public Set<String> refreshFavorites() throws NoSuchAlgorithmException, KeyManagementException {
//        Set<String> res = new HashSet<>();
//        refresh(res, "/favorites/refresh-materialized-views");
//        return res;
//    }

//    private void refresh(Set<String> res, String endpoint) throws NoSuchAlgorithmException, KeyManagementException {
//        Map result = getRestTemplate().postForObject(
//                getStatServerHost() + ":" + STAT_SERVER_PORT + endpoint,
//                new HashMap<>(), Map.class);
//        List<String> messages = (List) result.get("messages");
//        if (!messages.isEmpty()) {
//            for (String message : messages) {
//                System.out.println("- " + message);
//            }
//            res.addAll(messages);
//        }
//    }

    private void sendProductMessage(QueueMessage productMessage) {
        amqpTemplate.convertAndSend("DIRECT_EXCHANGE", "PRODUCTS_ROUTING_KEY", productMessage);
    }

    public Set<String> save(List<Offer> offers) throws SavingOffersToDBException {
        Set<String> res = new HashSet<>();
        try {
            List<List<Offer>> partition = Lists.partition(offers, 300);
            for (List<Offer> offerList : partition) {
                ArrayList<Offer> data = new ArrayList<>();
                data.addAll(offerList);
                sendProductMessage(new QueueMessage("AddProductsQueueParam", data, apiKey));
//                Map result = getRestTemplate().postForObject(
//                        getUrl(), requestData, Map.class);
//                List<String> messages = (List) result.get("messages");
//                if (!messages.isEmpty()) {
//                    log.warn("Not all products have been saved due to the following reasons:");
//                    for (String message : messages) {
//                        log.warn("- {}", message);
//                    }
//                    res.addAll(messages);
//                }
            }
        } catch (Exception e) {
            throw new SavingOffersToDBException("Saving to the queue failed!", e);
        }
        return res;
    }

//    private RestTemplate getRestTemplate() throws NoSuchAlgorithmException, KeyManagementException {
//        return restTemplate;
//    }

//    private String getUrl() {
//        String url = getStatServerHost() + ":" + STAT_SERVER_PORT + "/products";
//        if (sendToQueue) {
//            url += "/async";
//        }
//        return url;
//    }
//
//    private String getStatServerHost() {
//        return STAT_SERVER_HOST.contains("http") ? STAT_SERVER_HOST : "http://" + STAT_SERVER_HOST;
//    }

//    public void setSendToQueue(Boolean sendToQueue) {
//        this.sendToQueue = sendToQueue;
//    }
}
