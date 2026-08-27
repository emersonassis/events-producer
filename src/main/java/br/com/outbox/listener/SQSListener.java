package br.com.outbox.listener;

/**
 * Consumidor de filas SQS que extrai mensagens e as persiste como registros no outbox.
 */
import br.com.outbox.sender.message.sqs.SqsClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class SQSListener {

    public List<String> receive(String queueDlq) {

        List<Message> messages = buildSqsClient().receiveMessage(
                m -> m.messageAttributeNames("All")
                        .maxNumberOfMessages(10)
                        .queueUrl(queueDlq)
        ).messages();

        for(Message m: messages){
            Map<String, MessageAttributeValue> attrs = m.messageAttributes();
            MessageAttributeValue messageAttributeValue = attrs.get("outbox_id");

            // Aqui pode ser processado o atributo enviado
        }

        return messages.stream()
                .map(Message::body)
                .map(this::fetchMassage)
                .collect(Collectors.toList());
    }

    private String fetchMassage(String message) {
        return message;
    }

    private SqsClient buildSqsClient(){
        return SqsClientBuilder.buildSqsClient("");
    }
}
