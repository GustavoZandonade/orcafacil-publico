package br.com.gustavo.orcafacil.dto;
import jakarta.validation.constraints.*;
public record CustomerInput(
    @NotBlank @Size(max=120) String name,
    @Email @Size(max=180) String email,
    @Pattern(regexp="^(|[0-9]{14}|[0-9]{2}\\.[0-9]{3}\\.[0-9]{3}/[0-9]{4}-[0-9]{2})$",
        message="informe um CNPJ com 14 dígitos, com ou sem pontuação") String cnpj,
    @Size(max=300) String address,
    @Size(max=120) String responsibleName) { }
