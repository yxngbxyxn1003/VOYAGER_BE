package com.planty.entity.board;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_category",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"ai_message_id", "category_name"})})
public class AiCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_message_id", nullable = false)
    private AiMessage aiMessage;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

}