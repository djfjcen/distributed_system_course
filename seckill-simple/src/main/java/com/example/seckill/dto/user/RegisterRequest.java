package com.example.seckill.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "username can not be blank")
    @Size(min = 3, max = 50, message = "username length must be between 3 and 50")
    private String username;

    @NotBlank(message = "email can not be blank")
    @Email(message = "email format is invalid")
    private String email;

    @NotBlank(message = "password can not be blank")
    @Size(min = 6, max = 20, message = "password length must be between 6 and 20")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
