package com.fitproject.gestion.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento de notificación publicado hacia el Exchange de RabbitMQ.
 *
 * <p>La estructura debe coincidir exactamente con el {@code NotificationEvent}
 * de MS-Notificaciones para que el deserializador Jackson pueda procesarlo.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    /** Email del destinatario de la notificación. */
    private String recipientEmail;

    /** Nombre legible del destinatario. */
    private String recipientName;

    /** Asunto del email a enviar. */
    private String subject;

    /** Cuerpo del email con el detalle del evento. */
    private String body;

    /** Tipo de evento: {@code EVIDENCE_APPROVED}, {@code SALE_CONFIRMED}, etc. */
    private String eventType;
}
