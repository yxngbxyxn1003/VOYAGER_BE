package com.planty.entity.crop;

import com.planty.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;


// 작물 엔티티
@Entity
@Table(name = "crop")
@Getter @Setter
@ToString
public class Crop {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id") // FK: crop.user_id -> users.id
    private User user;

    private String name;
    private String cropImg;

    private LocalDate startAt;
    private LocalDate endAt;

    private String environment;
    private String temperature;
    private String height;
    private String howTo;

    private Boolean harvest;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @CreationTimestamp
    private LocalDateTime modifiedAt;
}
