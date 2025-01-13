package br.com.microservices.orchestrated.orderservice.core.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class EventConsumer {

    private final JsonUtil jsonUtil;

    @KafkaListener(groupId = "${spring.kafka.consumer.group-id}", topics = "${spring.kafka.topic.notify-ending}")
    public void consumeNotifyEndingEvent(String payload) {
        log.info("Recebendo evento de notificação final {} de notify-ending topic", payload);
        var event = jsonUtil.toEvent(payload);

    }
}
