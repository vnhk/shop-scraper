package com.bervan.shopwebscraper.save;

import com.bervan.shopwebscraper.Offer;
import com.bervan.shstat.queue.QueueMessage;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class QueueService {
    @Value("${stat-server.apiKey}")
    private String apiKey;

    @Autowired
    private AmqpTemplate amqpTemplate;

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
            }
        } catch (Exception e) {
            throw new SavingOffersToDBException("Saving to the queue failed!", e);
        }
        return res;
    }
}

