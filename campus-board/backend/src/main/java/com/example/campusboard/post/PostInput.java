package com.example.campusboard.post;

import jakarta.validation.constraints.NotBlank;

public record PostInput(@NotBlank String title, @NotBlank String content) {
}
