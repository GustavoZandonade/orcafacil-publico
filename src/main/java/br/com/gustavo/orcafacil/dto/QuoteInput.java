package br.com.gustavo.orcafacil.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record QuoteInput(
    @NotNull @Positive Long customerId,
    @NotBlank @Size(max=140) String title,
    @Size(max=2000) String description,
    @NotNull LocalDate validUntil,
    @NotNull @DecimalMin("0") @Digits(integer=14, fraction=2) BigDecimal discount,
    @PositiveOrZero int version,
    @NotEmpty @Size(max=50) List<@NotNull @Valid ItemInput> items) { }
