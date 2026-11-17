package br.com.gustavo.orcafacil.model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record Quote(long id, long customerId, String customerName, String title, String description,
    LocalDate validUntil, BigDecimal discount, Status status, int version,
    List<QuoteItem> items, BigDecimal subtotal, BigDecimal total) { }
