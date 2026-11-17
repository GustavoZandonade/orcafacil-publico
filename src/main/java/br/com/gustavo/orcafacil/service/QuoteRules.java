package br.com.gustavo.orcafacil.service;
import br.com.gustavo.orcafacil.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
public final class QuoteRules {
    private QuoteRules() { }
    public static BigDecimal subtotal(List<QuoteItem> items) {
        return items.stream().map(QuoteItem::lineTotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
    public static BigDecimal total(List<QuoteItem> items, BigDecimal discount) {
        BigDecimal subtotal = subtotal(items);
        if (discount.signum() < 0 || discount.compareTo(subtotal) > 0)
            throw new BusinessException(400, "O desconto deve estar entre zero e o subtotal.");
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }
    public static void checkTransition(Status current, Status target) {
        boolean allowed = switch (current) {
            case RASCUNHO -> target == Status.ENVIADO || target == Status.CANCELADO;
            case ENVIADO -> target == Status.APROVADO || target == Status.REJEITADO || target == Status.CANCELADO;
            default -> false;
        };
        if (!allowed) throw new BusinessException(409, "Transição de situação não permitida.");
    }
}
