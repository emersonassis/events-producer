package br.com.outbox.sender.http;

import br.com.outbox.dto.Outbox;
import br.com.outbox.enums.Event;
import br.com.outbox.enums.Status;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@QuarkusTest
class HTTPSenderTest {

    @Inject
    HTTPSender httpSender;

    Outbox outbox;

    @InjectMock
    Requester request;

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
    void whenSendPOSTRequest_WithSucess_ReturnEmptyError() throws Exception{
        when(request.request(any())).thenReturn(getHttpResponse(200));

        var error = httpSender.send(outbox);

        assertEquals("", error);
    }

    private Outbox initOutboxMock(){
        Faker faker = faker();

        Outbox outbox = new Outbox();
        outbox.setId(faker.number().randomNumber());

        outbox.setItem_type("Order::Order");
        outbox.setItem_id(faker.number().randomDigit());

        Gson a = new Gson();

        HashMap<String, String> mapJson = new HashMap<>();
        mapJson.put("uri", "http://localhost:33161/mockserver/dashboard");
        mapJson.put("method", "POST");

        HashMap<String, String> mapJsonHeaders = new HashMap<>();
        mapJsonHeaders.put("Ocp-Apim-Subscription-Key", "Ocp-Apim-Subscription-Key");
        mapJsonHeaders.put("api-key", "api-key");
        mapJson.put("headers", a.toJson(mapJsonHeaders));

        outbox.setDestiny(a.toJson(mapJson));

        mapJson = new HashMap<>();
        mapJson.put("payload", "{\"data\": {\"id\": \"6B1B427BC21F4159AEB09E8FD7669914\", \"status\": \"expired\"}");
        mapJson.put("event", "invoice.status_changed");

        outbox.setMessage(a.toJson(mapJson));

        outbox.setIntegrated(false);
        outbox.setTentatives(initTentatives);
        outbox.setStatus(Status.FOR_PROCESS.getValue());
        outbox.setEvent(Event.HTTP.getValue());

        return outbox;
    }

    private Faker faker(){
        return new Faker();
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private HashMap<String, String> getParams(Outbox outbox) {
        try {
            String destiny = outbox.getDestiny();
            var jsonMap = mapper.readValue(destiny, new TypeReference<Map<String, Object>>() {
            });

            HashMap<String, String> params = new HashMap<>();

            params.put("uri", String.valueOf(jsonMap.get("uri")));
            params.put("headers", String.valueOf(jsonMap.get("headers")));
            params.put("method", String.valueOf(jsonMap.get("method")));

            String message = outbox.getMessage();
            jsonMap = mapper.readValue(message, new TypeReference<Map<String, Object>>() {
            });
            String json = new Gson().toJson(jsonMap.get("payload"));
            params.put("payload", json);

            return params;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpResponse<String> getHttpResponse(int statusCode){
        return new HttpResponse<String>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public String body() {
                return "";
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return null;
            }
        };
    }
}