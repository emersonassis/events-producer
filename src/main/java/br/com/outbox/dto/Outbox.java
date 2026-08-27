package br.com.outbox.dto;

/**
 * Entidade JPA que mapeia a tabela outboxes, representando um registro do pattern outbox.
 */
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;

@Setter
@Getter
@Entity
@Table(name = "outboxes")
public class Outbox extends PanacheEntityBase {
    @Id
    private Long id;

    @Column(name = "item_type")
    private String item_type;

    @Column(name = "item_id")
    private Integer item_id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "destiny", columnDefinition = "jsonb", insertable = false, updatable = false)
    private String destiny;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "message", columnDefinition = "jsonb", insertable = false, updatable = false)
    private String message;

    @Column(name = "integrated")
    private Boolean integrated;

    @Column(name = "tentatives")
    private Integer tentatives;

    @Column(name = "status")
    private String status;

    @Column(name = "error")
    private String error;

    @Column(name = "event")
    private String event;

    @Column(name = "integration_type")
    private String integration_type;

    @Column(name = "created_at")
    private Date created_at;

    @Column(name = "process_in")
    private Date process_in;

    @Column(name = "claimed_until")
    private Date claimed_until;

    @Column(name = "processed_at")
    private Date processed_at;
}

