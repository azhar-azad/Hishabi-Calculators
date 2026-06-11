package dev.azhar.hishabi.platform.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank String email, @NotBlank @Size(max = 128) String password) {}
