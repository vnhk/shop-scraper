package com.bervan.shopwebscraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {
    @Bean
    public Queue logsQueue() {
        return new Queue("LOGS_QUEUE", true);
    }

    @Bean
    public DirectExchange logsDirectExchange() {
        return new DirectExchange("LOGS_DIRECT_EXCHANGE");
    }

    @Bean
    public Binding logsQueueBinding(Queue logsQueue, DirectExchange logsDirectExchange) {
        return BindingBuilder.bind(logsQueue).to(logsDirectExchange).with("LOGS_ROUTING_KEY");
    }

    @Bean
    public Queue productsQueue() {
        return new Queue("PRODUCTS_QUEUE", true);
    }

    @Bean
    public Queue scraperQueue() {
        return new Queue("SCRAPER_QUEUE", true);
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("DIRECT_EXCHANGE");
    }

    @Bean
    public DirectExchange scraperDirectExchange() {
        return new DirectExchange("SCRAPER_DIRECT_EXCHANGE");
    }

    @Bean
    public Binding productsQueueBinding(Queue productsQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(productsQueue).to(directExchange).with("PRODUCTS_ROUTING_KEY");
    }

    @Bean
    public Binding scraperQueueBinding(Queue scraperQueue, DirectExchange scraperDirectExchange) {
        return BindingBuilder.bind(scraperQueue).to(scraperDirectExchange).with("SCRAPER_ROUTING_KEY");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public DefaultJackson2JavaTypeMapper trustedClassMapper() {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        return typeMapper;
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(CachingConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(2);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
