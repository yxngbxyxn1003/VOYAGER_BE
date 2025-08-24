package com.planty.entity.crop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


// 작물 카테고리 엔티티
@Entity
@Table(name = "crop_category",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"crop_id", "category_name"})
        })
@Getter
@Setter
@NoArgsConstructor
public class CropCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
