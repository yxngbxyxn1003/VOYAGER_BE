package com.planty.repository.mypage;

import com.planty.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;


// 마이페이지 레포지토리
public interface MypageRepository extends JpaRepository<User, Integer> {
}
