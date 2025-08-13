package com.planty.repository.user;

import com.planty.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // 유저 ID 로그인
    Optional<User> findByUserId(String userId);

    // 유저 아이디 중복 검사
    boolean existsByUserId(String userId);

    // 유저 닉네임 중복 검사
    boolean existsByNickname(String nickname);
}
