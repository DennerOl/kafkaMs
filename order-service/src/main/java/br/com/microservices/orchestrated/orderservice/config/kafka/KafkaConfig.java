package br.com.microservices.orchestrated.orderservice.config.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

/* classe responsavel por fazer a comunição com kafka */
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private static final Integer PARTITION_COUNT = 1;
    private static final Integer REPLICA_COUNT = 1;

    // Endereço(s) dos servidores Kafka onde o consumidor se conecta.
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // O ID do grupo de consumidores, que indica a qual grupo este consumidor
    // pertence
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // consumir dados de uma partição pela primeira vez ou quando há perda de dados
    // de offset
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${spring.kafka.topic.start-saga}")
    private String startSagaTopic;

    @Value("${spring.kafka.topic.notify-ending}")
    private String notifyEndingTopic;

    // cria consumidores com configurações específicas para consumir as mensagens do
    // kafka.
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProps());
    }

    private Map<String, Object> consumerProps() {
        var props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        return props;
    }

    // cria produtores com configurações específicas para enviar as mensagens ao
    // kafka.
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    private Map<String, Object> producerProps() {
        var props = new HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    // configurado para produzir mensagens do tipo String (tanto para chave quanto
    // para valor).
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // cria topics de forma automatica
    private NewTopic buildTopic(String name) {
        return TopicBuilder
                .name(name)
                .partitions(PARTITION_COUNT)
                .replicas(REPLICA_COUNT)
                .build();
    }

    //
    @Bean
    public NewTopic startSagaTopic() {
        return buildTopic(startSagaTopic);
    }

    @Bean
    public NewTopic notifyEndingTopic() {
        return buildTopic(notifyEndingTopic);
    }
}