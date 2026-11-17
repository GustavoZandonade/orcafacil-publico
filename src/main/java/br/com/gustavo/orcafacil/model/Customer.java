package br.com.gustavo.orcafacil.model;

public record Customer(long id, String name, String email, String cnpj, String address, String responsibleName) {
    public String displayName() {
        return responsibleName == null || responsibleName.isBlank()
            ? name : responsibleName + " - " + name;
    }
}
