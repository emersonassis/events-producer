package br.com.outbox.sender.message.rabbitmq;

/**
 * Publicador que envia mensagens para exchanges/diretórios do RabbitMQ com persistência.
 */
import br.com.outbox.config.message.RabbitMQConfig;
import com.rabbitmq.client.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Slf4j
@ApplicationScoped
public class Publisher {

    @Inject
    RabbitMQConfig rabbitMQConfig;

    public void publish(HashMap<String, String> params) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQConfig.getHost());
        factory.setPort(rabbitMQConfig.getPort());
        factory.setUsername(rabbitMQConfig.getUsername());
        factory.setPassword(rabbitMQConfig.getPassword());

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("text/plain")
                    .deliveryMode(2)
                    .build();

            channel.exchangeDeclare(params.get("exchange_name"), BuiltinExchangeType.DIRECT, true);
            channel.queueBind(params.get("queue"), params.get("exchange_name"), params.get("routing_key"));

            channel.basicPublish(
                    params.get("exchange_name"),
                    params.get("routing_key"),
                    props,
                    params.get("payload").getBytes(StandardCharsets.UTF_8)
            );
        }
    }
}
