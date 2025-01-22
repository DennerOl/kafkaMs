package br.com.microservices.orchestrated.productvalidationservice.core.service;

import br.com.microservices.orchestrated.productvalidationservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.Event;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.History;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.productvalidationservice.core.model.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus.*;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@AllArgsConstructor
public class ProductValidationService {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final ProductRepository productRepository;
    private final ValidationRepository validationRepository;

    public void validateExistingProducts(Event event) {
        try {
            checkCurrentValidation(event);
            createValidation(event, true);
            handleSuccess(event);
        } catch (Exception ex) {
            log.error("Error trying to validate product: ", ex);
            handleFailCurrentNotExecuted(event, ex.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    // verifico se os produtos não estão vazios e se tem orderId e transaction Id
    private void validateProductsInformed(Event event) {
        if (isEmpty(event.getPayload()) || isEmpty(event.getPayload().getProducts())) {
            throw new ValidationException("A lista de produtos está vazia!");
        }
        if (isEmpty(event.getPayload().getId()) || isEmpty(event.getTransactionId())) {
            throw new ValidationException("OrderID e TransactionID devem ser informados!");
        }
    }

    // verifica os produtos (2)
    private void checkCurrentValidation(Event event) {
        validateProductsInformed(event);
        if (validationRepository.existsByOrderIdAndTransactionId(
                event.getOrderId(), event.getTransactionId())) {
            throw new ValidationException("Há outro transactionId para esta validação.");
        }
        event.getPayload().getProducts().forEach(product -> {
            validateProductInformed(product);
            validateExistingProduct(product.getProduct().getCode());
        });
    }

    // verifica se o produto foi informado
    private void validateProductInformed(OrderProducts product) {
        if (isEmpty(product.getProduct()) || isEmpty(product.getProduct().getCode())) {
            throw new ValidationException("O produto deve ser informado!");
        }
    }

    // verifica se os produtos existem
    private void validateExistingProduct(String code) {
        if (!productRepository.existsByCode(code)) {
            throw new ValidationException("O produto não existe no banco de dados!");
        }
    }

    // criar a validação no banco
    private void createValidation(Event event, boolean success) {
        var validation = Validation
                .builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .success(success)
                .build();
        validationRepository.save(validation);
    }

    // atualiza o evento com status de sucesso
    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Produtos validados com sucesso!");
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

    // trata o evento para lidar com rollback quando houver falha
    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Falha ao validar produtos: ".concat(message));
    }

    // dar um rollback no evento caso tenha falha
    public void rollbackEvent(Event event) {
        changeValidationToFail(event);
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback executado na validação do produto!");
        producer.sendEvent(jsonUtil.toJson(event));
    }

    // salva o evento com status de falha
    private void changeValidationToFail(Event event) {
        validationRepository
                .findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())
                .ifPresentOrElse(validation -> {
                    validation.setSuccess(false);
                    validationRepository.save(validation);
                },
                        () -> createValidation(event, false));
    }
}
