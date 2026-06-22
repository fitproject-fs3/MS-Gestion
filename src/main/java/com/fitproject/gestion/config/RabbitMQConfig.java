package com.fitproject.gestion.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración RabbitMQ del lado productor para MS-Gestion.
 *
 * <p>Declara el Exchange {@value #NOTIFICATIONS_EXCHANGE} y las constantes de
 * routing keys utilizadas al publicar eventos. Las colas y bindings son
 * responsabilidad exclusiva del consumidor (MS-Notificaciones).</p>
 */
@Configuration
public class RabbitMQConfig {

    /** Exchange compartido con MS-Notificaciones para eventos del ecosistema FitProject. */
    public static final String NOTIFICATIONS_EXCHANGE = "fit.notifications";

    /** Routing key para el evento de aprobación de evidencia. */
    public static final String EVIDENCE_APPROVED_KEY = "evidence.approved";

    /**
     * Declara el TopicExchange. Si ya existe (creado por MS-Notificaciones),
     * Spring AMQP verifica que los atributos coincidan sin volver a crearlo.
     *
     * @return instancia del exchange de notificaciones
     */
    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE);
    }

    /**
     * Conversor JSON para serializar los eventos publicados como mensajes AMQP.
     *
     * @return conversor Jackson2 para mensajes RabbitMQ
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configura el {@link RabbitTemplate} con el conversor JSON.
     *
     * @param connectionFactory fábrica de conexiones gestionada por Spring Boot
     * @return template configurado para publicar mensajes JSON
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
