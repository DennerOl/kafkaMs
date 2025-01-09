package br.com.microservices.orchestrated.paymentservice.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
// classe para converter a reposta para kafka e para os serviços
public class JsonUtil {

    private final ObjectMapper objectMapper;

    // espera um objeto e converte para json
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception ex) {
            return "";
        }
    }

    // espera uma string e converte para objeto
    public Event toEvent(String json) {
        try {
            return objectMapper.readValue(json, Event.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
