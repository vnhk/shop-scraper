package com.bervan.shopwebscraper.save;

import com.bervan.shopwebscraper.Offer;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class StatServerService {
    @Value("${stat-server.host:http://localhost}")
    private String STAT_SERVER_HOST = "http://localhost";

    @Value("${stat-server.port:8080}")
    private String STAT_SERVER_PORT = "8080";

    @Value("${send-to-queue:false}")
    private Boolean sendToQueue = false;

    @Autowired
    private RestTemplate restTemplate;

    public Set<String> refreshViews() throws SavingOffersToDBException {
        Set<String> res = new HashSet<>();
        try {
            refresh(res, "/products/refresh-materialized-views");
            res.addAll(refreshFavorites());
        } catch (Exception e) {
            throw new SavingOffersToDBException("Views could not be refreshed!", e);
        }
        return res;
    }

    public Set<String> refreshFavorites() {
        Set<String> res = new HashSet<>();
        refresh(res, "/favorites/refresh-materialized-views");
        return res;
    }

    private void refresh(Set<String> res, String endpoint) {
        Map result = getRestTemplate().postForObject(
                getStatServerHost() + ":" + STAT_SERVER_PORT + endpoint,
                new HashMap<>(), Map.class);
        List<String> messages = (List) result.get("messages");
        if (!messages.isEmpty()) {
            for (String message : messages) {
                System.out.println("- " + message);
            }
            res.addAll(messages);
        }
    }

    public Set<String> save(List<Offer> offers) throws SavingOffersToDBException {
        Set<String> res = new HashSet<>();
        try {
            List<List<Offer>> partition = Lists.partition(offers, 300);
            for (List<Offer> offerList : partition) {
                Map result = getRestTemplate().postForObject(
                        getUrl(), offerList, Map.class);
                List<String> messages = (List) result.get("messages");
                if (!messages.isEmpty()) {
                    System.out.println("Not all products have been saved due to the following reasons:");
                    for (String message : messages) {
                        System.out.println("- " + message);
                    }
                    res.addAll(messages);
                }
            }
        } catch (Exception e) {
            throw new SavingOffersToDBException("Saving to the database failed!", e);
        }
        return res;
    }

    private RestTemplate getRestTemplate() {
        if(restTemplate == null) {
            return new RestTemplate();
        }
        return restTemplate;
    }

    private String getUrl() {
        String url = getStatServerHost() + ":" + STAT_SERVER_PORT + "/products";
        if (sendToQueue) {
            url += "/async";
        }
        return url;
    }

    private String getStatServerHost() {
        return STAT_SERVER_HOST.contains("http") ? STAT_SERVER_HOST : "http://" + STAT_SERVER_HOST;
    }

    public void setSendToQueue(Boolean sendToQueue) {
        this.sendToQueue = sendToQueue;
    }
}
