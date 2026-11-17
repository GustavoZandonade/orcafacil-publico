package br.com.gustavo.orcafacil.repository;
import br.com.gustavo.orcafacil.dto.*;
import br.com.gustavo.orcafacil.model.*;
import br.com.gustavo.orcafacil.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.Statement;
import java.util.List;
@Repository
public class QuoteRepository {
    private final JdbcTemplate jdbc;
    public QuoteRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Customer> customers() {
        return jdbc.query("SELECT id, name, email, cnpj, address, responsible_name FROM customers ORDER BY name, id",
            (rs, row) -> customer(rs));
    }
    public Customer customer(long id) {
        return jdbc.query("SELECT id, name, email, cnpj, address, responsible_name FROM customers WHERE id=?",
            (rs, row) -> customer(rs), id)
            .stream().findFirst().orElseThrow(() -> new BusinessException(404, "Cliente não encontrado."));
    }
    private Customer customer(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Customer(rs.getLong("id"),rs.getString("name"),rs.getString("email"),rs.getString("cnpj"),
            rs.getString("address"),rs.getString("responsible_name"));
    }
    public Customer createCustomer(CustomerInput input) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("INSERT INTO customers(name,email,cnpj,address,responsible_name) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,input.name().trim());ps.setString(2,trimOrEmpty(input.email()));ps.setString(3,trimOrEmpty(input.cnpj()));
            ps.setString(4,trimOrEmpty(input.address()));ps.setString(5,trimOrEmpty(input.responsibleName()));
            return ps;
        }, key);
        return customer(key.getKey().longValue());
    }
    public Customer updateCustomer(long id, CustomerInput input) {
        int count=jdbc.update("UPDATE customers SET name=?,email=?,cnpj=?,address=?,responsible_name=? WHERE id=?",
            input.name().trim(),trimOrEmpty(input.email()),trimOrEmpty(input.cnpj()),trimOrEmpty(input.address()),
            trimOrEmpty(input.responsibleName()),id);
        if(count!=1) throw new BusinessException(404,"Cliente não encontrado.");
        return customer(id);
    }
    private String trimOrEmpty(String value) { return value == null ? "" : value.trim(); }
    public List<Quote> all() {
        return jdbc.queryForList("SELECT id FROM quotes ORDER BY id DESC", Long.class).stream().map(this::find).toList();
    }
    public Quote find(long id) {
        var items = jdbc.query("SELECT id,description,quantity,unit_price,status,image_data FROM quote_items WHERE quote_id=? ORDER BY id",
            (rs, row) -> new QuoteItem(rs.getLong("id"),rs.getString("description"),rs.getBigDecimal("quantity"),
                rs.getBigDecimal("unit_price"),ItemStatus.valueOf(rs.getString("status")),rs.getString("image_data")), id);
        return jdbc.query("""
            SELECT q.*, CASE WHEN TRIM(c.responsible_name) <> '' THEN c.responsible_name || ' - ' || c.name
                ELSE c.name END AS customer_name
            FROM quotes q JOIN customers c ON c.id=q.customer_id WHERE q.id=?
            """, (rs, row) -> new Quote(rs.getLong("id"), rs.getLong("customer_id"), rs.getString("customer_name"),
                rs.getString("title"),rs.getString("description"), rs.getDate("valid_until").toLocalDate(), rs.getBigDecimal("discount"),
                Status.valueOf(rs.getString("status")), rs.getInt("version"), items,
                QuoteRules.subtotal(items), QuoteRules.total(items, rs.getBigDecimal("discount"))), id)
            .stream().findFirst().orElseThrow(() -> new BusinessException(404, "Orçamento não encontrado."));
    }
    public long create(QuoteInput input) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps=connection.prepareStatement("INSERT INTO quotes(customer_id,title,description,valid_until,discount) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1,input.customerId()); ps.setString(2,input.title().trim());ps.setString(3,input.description()==null?"":input.description().trim());
            ps.setObject(4,input.validUntil()); ps.setBigDecimal(5,input.discount()); return ps;
        }, key);
        long id=key.getKey().longValue(); saveItems(id,input.items()); return id;
    }
    public void update(long id, QuoteInput input) {
        int count=jdbc.update("""
            UPDATE quotes SET customer_id=?,title=?,description=?,valid_until=?,discount=?,version=version+1
            WHERE id=? AND version=? AND status='RASCUNHO'
            """, input.customerId(),input.title().trim(),input.description()==null?"":input.description().trim(),
            input.validUntil(),input.discount(),id,input.version());
        if (count != 1) throw new BusinessException(409,"O orçamento foi alterado ou não está mais em rascunho. Recarregue a página.");
        jdbc.update("DELETE FROM quote_items WHERE quote_id=?",id);
        saveItems(id,input.items());
    }
    private void saveItems(long id,List<ItemInput> items) {
        for (var item:items) jdbc.update("INSERT INTO quote_items(quote_id,description,quantity,unit_price,image_data) VALUES (?,?,?,?,?)",
            id,item.description().trim(),item.quantity(),item.unitPrice(),item.imageData());
    }
    public void changeItemStatus(long quoteId,long itemId,ItemStatus status,int version) {
        int updated=jdbc.update("""
            UPDATE quote_items SET status=? WHERE id=? AND quote_id=?
            """,status.name(),itemId,quoteId);
        if(updated!=1) throw new BusinessException(404,"Item não encontrado neste orçamento.");
        int quoteUpdated=jdbc.update("""
            UPDATE quotes SET version=version+1 WHERE id=? AND version=? AND status='APROVADO'
            """,quoteId,version);
        if(quoteUpdated!=1) throw new BusinessException(409,"O orçamento foi alterado ou não está aprovado. Recarregue a página.");
    }
    public void changeStatus(long id,StatusInput input) {
        int count=jdbc.update("UPDATE quotes SET status=?,version=version+1 WHERE id=? AND version=?",
            input.status().name(),id,input.version());
        if(count != 1) throw new BusinessException(409,"O orçamento foi alterado. Recarregue a página.");
    }
}
