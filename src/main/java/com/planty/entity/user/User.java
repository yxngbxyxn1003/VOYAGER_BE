package com.planty.entity.user;

import com.planty.dto.user.SignupFormDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "users")
@Getter @Setter
@ToString
public class User {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    private Integer point = 0;
    private String profileImg;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BlockUser> blocks = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime modifiedAt;

    // 회원가입
    public static User createUser(SignupFormDto signupFormDto, PasswordEncoder passwordEncoder) {
        // 유저 객체 생성
        User user = new User();

        // 회원가입 데이터 세팅
        user.setUserId(signupFormDto.getUserId());
        String pwd = passwordEncoder.encode(signupFormDto.getPassword());
        user.setPassword(pwd);
        user.setNickname(signupFormDto.getNickname());

        // 반환
        return user;
    }
}
