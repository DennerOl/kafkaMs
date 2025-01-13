package br.com.microservices.orchestrated.productvalidationservice.core.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductValidationConsumer {

    private final JsonUtil jsonUtil;

    @KafkaListener(groupId = "${spring.kafka.consumer.group-id}", topics = "${spring.kafka.topic.product-validation-success}")
    public void consumeSuccessEvent(String payload) {
        log.info("Recebendo evento de sucesso {} de product-validation-success topic", payload);
        var event = jsonUtil.toEvent(payload);

    }

    @KafkaListener(groupId = "${spring.kafka.consumer.group-id}", topics = "${spring.kafka.topic.product-validation-fail}")
    public void consumeFailEvent(String payload) {
        log.info("Recebendo evento de reversão {} de product-validation-fail topic", payload);
        var event = jsonUtil.toEvent(payload);

    }
}
