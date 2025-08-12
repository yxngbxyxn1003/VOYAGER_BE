package com.planty.service.user;

import com.planty.entity.user.User;
import com.planty.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;


// 회원가입, 로그인, 로그아웃
@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    // 회원가입
    public User saveUser(User user) {
        validateDuplicateId(user);
        validateDuplicateNickname(user);
        return userRepository.save(user);
    }

    // userId 중복 확인
    private void validateDuplicateId(User user) {
        if (userRepository.existsByUserId(user.getUserId())) {
            throw new ResponseStatusException(CONFLICT, "DUPLICATE_USER_ID");
        }
    }

    // 닉네임 중복 확인
    private void validateDuplicateNickname(User user) {
        if (userRepository.existsByNickname(user.getNickname())) {
            throw new ResponseStatusException(CONFLICT, "DUPLICATE_NICKNAME");
        }
    }

    // 로그인 시 호출되는 메서드
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User u = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 권한 없이 기본 USER 권한만 부여
        return new org.springframework.security.core.userdetails.User(
                u.getUserId(),
                u.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
