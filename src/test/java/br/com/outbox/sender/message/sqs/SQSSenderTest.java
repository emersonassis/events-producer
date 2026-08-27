package br.com.outbox.sender.message.sqs;

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
import software.amazon.awssdk.core.exception.SdkClientException;

import java.util.HashMap;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@Slf4j
@QuarkusTest
class SQSSenderTest {

    @Inject
    SQSSender sqsSender;

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
        assertEquals(sqsSender.typeSender(), Event.SQS);
    }

    @Test
    void whenPublishMessage_WithSucess_ReturnEmptyError() {
        doNothing().when(publisher).publish(any());

        var error = sqsSender.send(outbox);

        assertEquals("", error);
    }

    @Test
    void whenTheDestiny_IsOutOfPattern_ReturnParseError() {
        outbox.setDestiny("destino-fora-do-padrao");

        var error = sqsSender.send(outbox);

        assertTrue(error.startsWith("Parse falhou"));
    }

    @Test
    void whenPublishMessage_Fails_ReturnConnectionError() {
        doThrow(SdkClientException.create("conexão recusada")).when(publisher).publish(any());

        var error = sqsSender.send(outbox);

        assertTrue(error.startsWith("Conexão falhou"));
    }

    private Outbox initOutboxMock(){
        Faker faker = faker();

        Outbox outbox = new Outbox();
        outbox.setId(faker.number().randomNumber());

        outbox.setItem_type("Order::Order");
        outbox.setItem_id(faker.number().randomDigit());

        var queue = "https://sqs.us-east-1.amazonaws.com/770313171267/test-send-msm-events-producer";

        Gson a = new Gson();
        HashMap<String, String> mapJson = new HashMap<>();
        mapJson.put("queue_url", queue);
        mapJson.put("region", "us-east-1");
        outbox.setDestiny(a.toJson(mapJson));

        mapJson = new HashMap<>();
        mapJson.put("payload", "{\"cart_id\":4379931,\"token\": null,\"balance_discount\":10095.93,\"clear_sale_fingerprint\": null, \"credit_card_info\": null, \"coupon\": null}");
        outbox.setMessage(a.toJson(mapJson));

        outbox.setIntegrated(false);
        outbox.setTentatives(initTentatives);
        outbox.setStatus(Status.FOR_PROCESS.getValue());
        outbox.setEvent(Event.SQS.getValue());

        return outbox;
    }

    private Faker faker(){
        return new Faker();
    }
}