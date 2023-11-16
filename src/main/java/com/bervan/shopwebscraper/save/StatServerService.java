package com.bervan.shopwebscraper.save;

import com.bervan.shopwebscraper.Offer;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class StatServerService {
    @Value("${stat-server.host}")
    private final String STAT_SERVER_HOST = "http://localhost";

    @Value("${stat-server.port}")
    private final String STAT_SERVER_PORT = "8080";

    private final RestTemplate restTemplate = new RestTemplate();

    public void save(List<Offer> offers) throws SavingOffersToDBException {
        System.out.println("Saving to the database...");
        try {
            List<List<Offer>> partition = Lists.partition(offers, 300);
            int i = 1;
            for (List<Offer> offerList : partition) {
                Map result = restTemplate.postForObject(
                        STAT_SERVER_HOST + ":" + STAT_SERVER_PORT + "/products", offerList, Map.class);
//                System.out.printf("Saved part (%d/%d) of data (%s products) to the database.\n",
//                        i,
//                        partition.size(),
//                        result.get("savedProducts"));
                List<String> messages = (List) result.get("messages");
                if (!messages.isEmpty()) {
                    System.out.println("Not all products have been saved due to the following reasons:");
                    for (String message : messages) {
                        System.out.println("- " + message);
                    }
                }
                i++;
            }
        } catch (Exception e) {
            throw new SavingOffersToDBException("Saving to the database failed!", e);
        }
        System.out.println("Saved to the database.");
    }
}
