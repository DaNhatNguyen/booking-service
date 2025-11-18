package com.example.booking_service.configuration;

import com.example.booking_service.entity.User;
import com.example.booking_service.enums.Role;
import com.example.booking_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j // ano de log
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    // chạy moi khi app start
    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
//            if (userRepository.findByEmail("admin").isEmpty()) {
//                var roles = new HashSet<String>();
//                roles.add(Role.ADMIN.name());
//
//                User user = User.builder()
//                        .email("admin@gmail.com")
//                        .password(passwordEncoder.encode("admin"))
//                        .roles(roles)
//                        .build();
//
//                userRepository.save(user);
//                log.warn("User admin has been created with default password: admin, please changes it");
//            }
        };
    }
}
