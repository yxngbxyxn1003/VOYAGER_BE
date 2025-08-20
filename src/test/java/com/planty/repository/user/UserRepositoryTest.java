package com.planty.repository.user;

import com.planty.entity.user.User;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "jwt.secret=test-secret-key-for-testing-only",
    "upload-dir=test-uploads",
    "parameter-store.enabled=false",
    "ai.openai.api-key=test-key",
    "ai.openai.timeout=30",
    "ai.openai.maxRetries=1"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveUser() {
        User user = new User();
        user.setUserId("testUser");
        user.setPassword("pass");
        user.setNickname("nickname");
        userRepository.save(user);

        System.out.println(user.toString());
    }
}
