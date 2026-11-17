package br.com.gustavo.orcafacil.controller;
import br.com.gustavo.orcafacil.dto.*;
import br.com.gustavo.orcafacil.model.*;
import br.com.gustavo.orcafacil.service.ExcelExportService;
import br.com.gustavo.orcafacil.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;
import java.util.List;
@RestController
@RequestMapping("/api")
public class QuoteController {
    private final QuoteService service;
    private final ExcelExportService excelExportService;
    public QuoteController(QuoteService service, ExcelExportService excelExportService) {
        this.service=service; this.excelExportService=excelExportService;
    }
    @GetMapping("/export/quotes.xlsx")
    public ResponseEntity<byte[]> exportQuotes() { return excel("orcamentos.xlsx",excelExportService.quotes()); }
    @GetMapping("/export/customers.xlsx")
    public ResponseEntity<byte[]> exportCustomers() { return excel("clientes.xlsx",excelExportService.customers()); }
    @GetMapping("/export/production.xlsx")
    public ResponseEntity<byte[]> exportProduction() { return excel("producao.xlsx",excelExportService.production()); }
    @GetMapping("/customers") public List<Customer> customers() { return service.customers(); }
    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
    public Customer createCustomer(@Valid @RequestBody CustomerInput body) { return service.createCustomer(body); }
    @PutMapping("/customers/{id}")
    public Customer updateCustomer(@PathVariable long id,@Valid @RequestBody CustomerInput body) { return service.updateCustomer(id,body); }
    @GetMapping("/quotes") public List<Quote> quotes() { return service.all(); }
    @GetMapping("/quotes/{id}") public Quote find(@PathVariable long id) { return service.find(id); }
    @PostMapping("/quotes") @ResponseStatus(HttpStatus.CREATED)
    public Quote create(@Valid @RequestBody QuoteInput body) { return service.create(body); }
    @PutMapping("/quotes/{id}")
    public Quote update(@PathVariable long id,@Valid @RequestBody QuoteInput body) { return service.update(id,body); }
    @PatchMapping("/quotes/{id}/status")
    public Quote status(@PathVariable long id,@Valid @RequestBody StatusInput body) { return service.changeStatus(id,body); }
    @PatchMapping("/quotes/status")
    public List<Quote> bulkStatus(@Valid @RequestBody BulkStatusInput body) { return service.changeStatuses(body); }
    @PatchMapping("/quotes/{quoteId}/items/{itemId}/status")
    public Quote itemStatus(@PathVariable long quoteId,@PathVariable long itemId,@Valid @RequestBody ItemStatusInput body) {
        return service.changeItemStatus(quoteId,itemId,body);
    }
    private ResponseEntity<byte[]> excel(String filename,byte[] content) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header("Content-Disposition",ContentDisposition.attachment().filename(filename,StandardCharsets.UTF_8).build().toString())
            .body(content);
    }
}
