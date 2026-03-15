package com.example.seckill.service;

import com.example.seckill.dto.user.LoginRequest;
import com.example.seckill.dto.user.RegisterRequest;
import com.example.seckill.entity.User;
import com.example.seckill.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        return userRepository.save(user);
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new IllegalArgumentException("username or password is incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("username or password is incorrect");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());
        result.put("message", "login success");
        return result;
    }
}
