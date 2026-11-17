package br.com.gustavo.orcafacil;
import br.com.gustavo.orcafacil.model.*;
import br.com.gustavo.orcafacil.service.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QuoteRulesTest {
    private QuoteItem item(String quantity,String price) { return new QuoteItem("Serviço",new BigDecimal(quantity),new BigDecimal(price)); }
    @Test void calculaVariosItensComDesconto() {
        var items=List.of(item("2","100.00"),item("3","50.00"));
        assertEquals(new BigDecimal("350.00"),QuoteRules.subtotal(items));
        assertEquals(new BigDecimal("325.00"),QuoteRules.total(items,new BigDecimal("25.00")));
    }
    @Test void somaDecimaisSemErroDePontoFlutuante() {
        assertEquals(new BigDecimal("0.30"),QuoteRules.total(List.of(item("3","0.10")),BigDecimal.ZERO));
    }
    @Test void arredondaCadaLinhaAntesDeSomar() {
        var items=List.of(item("0.125","0.20"),item("0.125","0.20"));
        assertEquals(new BigDecimal("0.06"),QuoteRules.subtotal(items));
    }
    @Test void aceitaDescontoIgualAoSubtotal() {
        assertEquals(new BigDecimal("0.00"),QuoteRules.total(List.of(item("1","10")),new BigDecimal("10")));
    }
    @Test void rejeitaDescontoExcessivo() {
        assertThrows(BusinessException.class,()->QuoteRules.total(List.of(item("1","10")),new BigDecimal("10.01")));
    }
    @Test void rejeitaDescontoNegativo() {
        assertThrows(BusinessException.class,()->QuoteRules.total(List.of(item("1","10")),new BigDecimal("-1")));
    }
    @Test void permiteFluxoDeAprovacao() {
        assertDoesNotThrow(()->QuoteRules.checkTransition(Status.RASCUNHO,Status.ENVIADO));
        assertDoesNotThrow(()->QuoteRules.checkTransition(Status.ENVIADO,Status.APROVADO));
    }
    @Test void impedeAprovarSemEnviar() {
        assertThrows(BusinessException.class,()->QuoteRules.checkTransition(Status.RASCUNHO,Status.APROVADO));
    }
    @Test void impedeReaberturaDeAprovado() {
        assertThrows(BusinessException.class,()->QuoteRules.checkTransition(Status.APROVADO,Status.RASCUNHO));
    }
}
