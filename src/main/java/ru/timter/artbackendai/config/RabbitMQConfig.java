package ru.timter.artbackendai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${art.exchange}")
    private String exchange;

    @Value("${art.analysis.queue}")
    private String analysisQueue;

    @Value("${art.analysis.routing-key}")
    private String analysisRoutingKey;

    @Value("${art.results.queue}")
    private String resultsQueue;

    @Value("${art.results.routing-key}")
    private String resultsRoutingKey;

    @Bean
    public DirectExchange artExchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Queue analysisQueue() {
        return QueueBuilder.durable(analysisQueue).build();
    }

    @Bean
    public Queue resultsQueue() {
        return QueueBuilder.durable(resultsQueue).build();
    }

    @Bean
    public Binding analysisBinding(Queue analysisQueue, DirectExchange artExchange) {
        return BindingBuilder.bind(analysisQueue).to(artExchange).with(analysisRoutingKey);
    }

    @Bean
    public Binding resultsBinding(Queue resultsQueue, DirectExchange artExchange) {
        return BindingBuilder.bind(resultsQueue).to(artExchange).with(resultsRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
