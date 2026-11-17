package br.com.gustavo.orcafacil.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record ItemInput(
    @NotBlank @Size(max=200) String description,
    @NotNull @DecimalMin("0.001") @DecimalMax("10000") @Digits(integer=5, fraction=3) BigDecimal quantity,
    @NotNull @DecimalMin("0") @DecimalMax("1000000") @Digits(integer=7, fraction=2) BigDecimal unitPrice,
    @Size(max=700000) String imageData) { }
