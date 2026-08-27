package br.com.outbox.sender.message.sqs;

/**
 * Publicador que envia mensagens para filas do Amazon SQS, com atributos customizados.
 */
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class Publisher {

    private SqsClient sqs;

    public void publish(HashMap<String, String> params) throws SdkClientException, QueueDoesNotExistException {
        var queueUrl = params.get("queue_url");
        var region = params.get("region");
        var message = params.get("payload");
        long outboxId = Long.parseLong(params.get("outbox_id"));

        sqsClient(region);

        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        var messageAttributeValue = MessageAttributeValue
                .builder()
                .dataType("Number")
                .stringValue(String.valueOf(outboxId))
                .build();
        messageAttributes.put("outbox_id", messageAttributeValue);

        SendMessageResponse response = this.sqs.sendMessage(
                msm -> msm.queueUrl(queueUrl)
                        .messageBody(message)
                        .messageAttributes(messageAttributes)
        );

        Response.ok().entity(response.messageId()).build();
    }

    private void sqsClient(String region) throws SdkClientException {
        this.sqs = SqsClientBuilder.buildSqsClient(region);
    }
}
