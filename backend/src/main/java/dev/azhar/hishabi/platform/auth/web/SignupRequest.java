package dev.azhar.hishabi.platform.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank
                @Size(min = 8, max = 128)
                @Pattern(
                        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)\\S+$",
                        message =
                                "must contain at least one uppercase letter, one lowercase letter, and one digit")
                String password) {}
