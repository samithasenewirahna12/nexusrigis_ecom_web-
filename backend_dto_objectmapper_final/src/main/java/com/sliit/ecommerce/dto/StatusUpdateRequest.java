package com.sliit.ecommerce.dto;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull Boolean enabled) {}