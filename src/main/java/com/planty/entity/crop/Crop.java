package com.planty.entity.crop;

import com.planty.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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

    @OneToMany(mappedBy = "crop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CropCategory> categories = new ArrayList<>();


    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime modifiedAt;
}
