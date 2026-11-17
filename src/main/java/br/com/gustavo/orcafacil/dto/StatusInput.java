package br.com.gustavo.orcafacil.dto;
import br.com.gustavo.orcafacil.model.Status;
import jakarta.validation.constraints.*;
public record StatusInput(@NotNull Status status, @PositiveOrZero int version) { }
