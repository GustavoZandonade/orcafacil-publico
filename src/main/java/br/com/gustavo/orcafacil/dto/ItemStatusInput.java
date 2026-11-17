package br.com.gustavo.orcafacil.dto;

import br.com.gustavo.orcafacil.model.ItemStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ItemStatusInput(@NotNull ItemStatus status, @PositiveOrZero int version) { }
