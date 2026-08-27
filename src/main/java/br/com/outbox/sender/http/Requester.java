package br.com.outbox.sender.http;

/**
 * Cliente HTTP que monta e executa requisições REST com base nos parâmetros do registro.
 */
import br.com.outbox.config.ConfigClass;
import br.com.outbox.dto.Outbox;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@ApplicationScoped
public class Requester {
    @Inject
    private ConfigClass configClass;

    public HttpResponse<String> request(Outbox outbox) throws Exception{
        HashMap<String, String> params = getParams(outbox);

        var uri = params.get("uri");
        var method = params.get("method");
        var payload = params.get("payload");

        var httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(configClass.getHttpSenderTimeout()))
                .build();
        var bodyPublishers = HttpRequest.BodyPublishers.ofString(payload);

        var listHeaders = listHeaders(outbox.getDestiny()).toArray(String[]::new);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .headers(listHeaders)
                .timeout(java.time.Duration.ofSeconds(configClass.getHttpSenderTimeout()));

        if (method.equals("POST")) {
            request.POST(bodyPublishers);
        } else if (method.equals("PUT")) {
            request.PUT(bodyPublishers);
        }

        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HashMap<String, String> getParams(Outbox outbox) {
        HashMap<String, String> params = new HashMap<>();

        JSONObject inputJSONOBject = destinyJSONObject(outbox.getDestiny());

        params.put("uri", inputJSONOBject.get("uri").toString());
        params.put("method", inputJSONOBject.get("method").toString());
        params.put("payload", outbox.getMessage());

        return params;
    }

    private List<String> listHeaders(String destiny){
        JSONObject destinyJSONObject = destinyJSONObject(destiny);
        JSONObject headersJSONObject = destinyJSONObject(destinyJSONObject.get("headers").toString());

        var listHeaders = new ArrayList<String>();
        listHeaders.add("Content-Type");
        listHeaders.add("application/json");

        var keys = headersJSONObject.keys();
        while (keys.hasNext()) {
            var nextKeys = (String) keys.next();
            listHeaders.add(nextKeys);
            listHeaders.add(headersJSONObject.get(nextKeys).toString());
        }

        return listHeaders;
    }

    private JSONObject destinyJSONObject(String destiny){
        return new JSONObject(destiny);
    }
}
