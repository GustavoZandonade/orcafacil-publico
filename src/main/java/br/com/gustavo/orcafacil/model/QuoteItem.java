package br.com.gustavo.orcafacil.model;
import java.math.BigDecimal;
import java.math.RoundingMode;
public record QuoteItem(Long id, String description, BigDecimal quantity, BigDecimal unitPrice,
    ItemStatus status, String imageData) {
    public QuoteItem(String description, BigDecimal quantity, BigDecimal unitPrice) {
        this(null, description, quantity, unitPrice, ItemStatus.PENDENTE, null);
    }

    public BigDecimal lineTotal() { return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP); }
}
