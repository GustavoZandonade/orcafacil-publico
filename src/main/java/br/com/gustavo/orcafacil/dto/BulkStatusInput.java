package br.com.gustavo.orcafacil.dto;

import br.com.gustavo.orcafacil.model.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkStatusInput(
    @NotNull Status status,
    @NotEmpty @Size(max=100) List<@NotNull @Valid QuoteVersionInput> quotes) { }
