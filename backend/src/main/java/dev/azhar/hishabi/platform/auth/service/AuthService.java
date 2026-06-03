package dev.azhar.hishabi.platform.auth.service;

import dev.azhar.hishabi.platform.auth.model.User;
import dev.azhar.hishabi.platform.auth.repository.UserRepository;
import dev.azhar.hishabi.platform.auth.web.SignupRequest;
import dev.azhar.hishabi.platform.error.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(SignupRequest request) {

        User user =
                User.builder()
                        .email(request.email())
                        .passwordHash(passwordEncoder.encode(request.password()))
                        .build();

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Email already registered.");
        }
    }
}
