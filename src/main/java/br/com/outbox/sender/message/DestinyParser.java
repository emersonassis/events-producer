package br.com.outbox.sender.message;

/**
 * Parser que extrai parâmetros de roteamento a partir do campo JSON destiny do registro.
 */
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class DestinyParser {

    @Inject
    ObjectMapper mapper;

    public Map<String, String> parse(String destinyJson) {
        try {
            Map<String, Object> raw = mapper.readValue(
                    destinyJson,
                    new TypeReference<Map<String, Object>>() {
                    });
            Map<String, String> result = new HashMap<>();
            raw.forEach((key, value) -> result.put(key, String.valueOf(value)));
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("destiny inválido", e);
        }
    }
}
