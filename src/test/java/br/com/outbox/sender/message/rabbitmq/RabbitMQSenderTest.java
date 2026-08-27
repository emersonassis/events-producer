package br.com.outbox.sender.message.rabbitmq;

import br.com.outbox.dto.Outbox;
import br.com.outbox.enums.Event;
import br.com.outbox.enums.Status;
import com.github.javafaker.Faker;
import com.google.gson.Gson;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@Slf4j
@QuarkusTest
class RabbitMQSenderTest {

    @Inject
    RabbitMQSender rabbitMQSender;

    @InjectMock
    Publisher publisher;

    Outbox outbox;

    static int initTentatives;

    @BeforeEach
    void setUp() {
        this.outbox = initOutboxMock();
    }

    @AfterEach
    void tearDown() {
    }

    @BeforeAll
    public static void init() {
        Random r = new Random();
        initTentatives = r.nextInt(0, 2);
    }

    @Test
    void typeSender() {
        assertEquals(rabbitMQSender.typeSender(), Event.RABBITMQ);
    }

    @Test
    void whenPublishMessage_WithSucess_ReturnEmptyError() throws Exception {
        doNothing().when(publisher).publish(any());

        var error = rabbitMQSender.send(outbox);

        assertEquals("", error);
    }

    @Test
    void whenPublishMessage_Fails_ReturnPublicationError() throws Exception {
        doThrow(new IOException()).when(publisher).publish(any());

        var error = rabbitMQSender.send(outbox);

        assertTrue(error.startsWith("Publicação falhou"));
    }

    @Test
    void whenTheDestiny_IsOutOfPattern_ReturnParseError() {
        outbox.setDestiny("destino-fora-do-padrao");

        var error = rabbitMQSender.send(outbox);

        assertTrue(error.startsWith("Parse falhou"));
    }

    private Outbox initOutboxMock(){
        Faker faker = faker();

        Outbox outbox = new Outbox();
        outbox.setId(faker.number().randomNumber());

        outbox.setItem_type("Order::Order");
        outbox.setItem_id(faker.number().randomDigit());

        Gson a = new Gson();

        HashMap<String, String> mapJson = new HashMap<>();
        mapJson.put("routing_key", "product_manager.order.close_order");
        mapJson.put("exchange_name", "product_manager.order");
        mapJson.put("queue", "close_order.order");
        outbox.setDestiny(a.toJson(mapJson));

        mapJson = new HashMap<>();
        mapJson.put("payload", "{\"cart_id\":4379931,\"token\": null,\"balance_discount\":10095.93,\"clear_sale_fingerprint\": null, \"credit_card_info\": null, \"coupon\": null}");
        outbox.setMessage(a.toJson(mapJson));

        outbox.setIntegrated(false);
        outbox.setTentatives(initTentatives);
        outbox.setStatus(Status.FOR_PROCESS.getValue());
        outbox.setEvent(Event.RABBITMQ.getValue());

        return outbox;
    }

    private Faker faker(){
        return new Faker();
    }
}
