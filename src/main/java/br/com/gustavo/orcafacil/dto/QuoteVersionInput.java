package br.com.gustavo.orcafacil.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record QuoteVersionInput(@Positive long id, @PositiveOrZero int version) { }
