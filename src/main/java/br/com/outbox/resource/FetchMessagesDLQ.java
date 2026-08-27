package br.com.outbox.resource;

/**
 * Endpoint REST que drena filas SQS DLQ e reinsere as mensagens como registros outbox.
 */
import br.com.outbox.listener.SQSListener;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

@Slf4j
@Path("/fetch_message_dlq")
public class FetchMessagesDLQ {

    @Inject
    SQSListener sqsListener;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ClientHeaderParam(name = "queue_dlq", value = "{queue_dlq}")
    public String fetchMessageDQL(@HeaderParam("queue_dlq") String queueDlq){
        sqsListener.receive(queueDlq);

        return "Messagem processada da fila " + queueDlq;
    }
}