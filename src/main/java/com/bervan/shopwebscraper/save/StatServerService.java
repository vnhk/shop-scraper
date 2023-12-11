package com.bervan.shopwebscraper.save;

import com.bervan.shopwebscraper.Offer;
import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class StatServerService {
    @Value("${stat-server.host:http://localhost}")
    private String STAT_SERVER_HOST = "http://localhost";

    @Value("${stat-server.port:8080}")
    private String STAT_SERVER_PORT = "8080";

    private RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void config() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        restTemplate = new RestTemplate(factory);
    }

    public Set<String> refreshViews() throws SavingOffersToDBException {
        Set<String> res = new HashSet<>();
        try {
            Map result = restTemplate.postForObject(
                    getStatServerHost() + ":" + STAT_SERVER_PORT + "/products/refresh-materialized-views",
                    new HashMap<>(), Map.class);
            List<String> messages = (List) result.get("messages");
            if (!messages.isEmpty()) {
                System.out.println("Views could not be refreshed:");
                for (String message : messages) {
                    System.out.println("- " + message);
                }
                res.addAll(messages);
            }
        } catch (Exception e) {
            throw new SavingOffersToDBException("Saving to the database failed!", e);
        }
        return res;
    }

    public Set<String> save(List<Offer> offers) throws SavingOffersToDBException {
        Set<String> res = new HashSet<>();
        try {
            List<List<Offer>> partition = Lists.partition(offers, 300);
            for (List<Offer> offerList : partition) {
                Map result = restTemplate.postForObject(
                        getStatServerHost() + ":" + STAT_SERVER_PORT + "/products", offerList, Map.class);
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

    private String getStatServerHost() {
        return STAT_SERVER_HOST.contains("http") ? STAT_SERVER_HOST : "http://" + STAT_SERVER_HOST;
    }
}
