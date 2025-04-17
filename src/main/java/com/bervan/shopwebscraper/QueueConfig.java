package com.bervan.shopwebscraper;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {
    @Bean
    public Queue productsQueue() {
        return new Queue("PRODUCTS_QUEUE", true);
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("DIRECT_EXCHANGE");
    }

    @Bean
    public Binding productsQueueBinding(Queue productsQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(productsQueue).to(directExchange).with("PRODUCTS_ROUTING_KEY");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(trustedClassMapper());
        return converter;
    }

    @Bean
    public DefaultJackson2JavaTypeMapper trustedClassMapper() {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.bervan.shopwebscraper");
        return typeMapper;
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(CachingConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
