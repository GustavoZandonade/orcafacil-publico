package br.com.gustavo.orcafacil;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.io.ByteArrayInputStream;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1", "spring.sql.init.mode=always"})
class ApiIntegrationTest {
    @Value("${local.server.port}") int port;
    final HttpClient client=HttpClient.newHttpClient();
    final JsonMapper json=JsonMapper.builder().build();
    long customerId;
    HttpResponse<String> request(String method,String path,String body) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
        if(body!=null)b.header("Content-Type","application/json");
        return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<byte[]> requestBytes(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
    }
    long id(HttpResponse<String> r){return json.readTree(r.body()).get("id").asLong();}
    @BeforeEach void customer() throws Exception {
        var r=request("POST","/api/customers","{\"name\":\"Cliente de teste\",\"email\":\"teste@example.com\"}");
        assertEquals(201,r.statusCode());customerId=id(r);
    }
    String payload(long clientId,int version,String discount,String items) {
        return """
            {"customerId":%d,"title":"Orçamento de teste","description":"Detalhes da proposta","validUntil":"2026-11-30","discount":%s,"version":%d,"items":%s}
            """.formatted(clientId,discount,version,items);
    }
    String items(){return """
        [{"description":"Manutenção","quantity":2,"unitPrice":100.00},{"description":"Instalação","quantity":3,"unitPrice":50.00}]
        """;}
    long create() throws Exception {var r=request("POST","/api/quotes",payload(customerId,0,"25",items()));assertEquals(201,r.statusCode(),r.body());return id(r);}
    @Test void criaConsultaEConfereTotal() throws Exception {
        long id=create();var r=request("GET","/api/quotes/"+id,null);
        assertEquals(200,r.statusCode());assertEquals("325.0",json.readTree(r.body()).get("total").asText());
        assertEquals(2,json.readTree(r.body()).get("items").size());
        assertEquals("Detalhes da proposta",json.readTree(r.body()).get("description").asText());
    }
    @Test void editaCadastroDeCliente() throws Exception {
        var r=request("PUT","/api/customers/"+customerId,
            "{\"name\":\"Empresa Exemplo\",\"email\":\"novo@example.com\",\"cnpj\":\"12345678000199\",\"address\":\"Rua A, 123\",\"responsibleName\":\"Maria Silva\"}");
        assertEquals(200,r.statusCode(),r.body());
        var customer=json.readTree(r.body());
        assertEquals("Empresa Exemplo",customer.get("name").asText());
        assertEquals("novo@example.com",customer.get("email").asText());
        assertEquals("12345678000199",customer.get("cnpj").asText());
        assertEquals("Rua A, 123",customer.get("address").asText());
        assertEquals("Maria Silva",customer.get("responsibleName").asText());
    }
    @Test void mostraResponsavelEEmpresaNoOrcamento() throws Exception {
        var created=request("POST","/api/customers",
            "{\"name\":\"Empresa Exemplo\",\"responsibleName\":\"Maria Silva\"}");
        long id=id(created);
        var quote=request("POST","/api/quotes",payload(id,0,"0",items()));
        assertEquals(201,quote.statusCode(),quote.body());
        assertEquals("Maria Silva - Empresa Exemplo",json.readTree(quote.body()).get("customerName").asText());
    }
    @Test void rejeitaCnpjComFormatoInvalido() throws Exception {
        assertEquals(400,request("POST","/api/customers",
            "{\"name\":\"Empresa Exemplo\",\"cnpj\":\"12.345\"}").statusCode());
    }
    @Test void salvaImagemDeItem() throws Exception {
        String image="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=";
        String items=items().replace("\"description\":\"Manutenção\"","\"description\":\"Manutenção\",\"imageData\":\""+image+"\"");
        long id=id(request("POST","/api/quotes",payload(customerId,0,"0",items)));
        var saved=json.readTree(request("GET","/api/quotes/"+id,null).body());
        assertEquals(image,saved.get("items").get(0).get("imageData").asText());
    }
    @Test void exportaAbasEmArquivosExcel() throws Exception {
        String image="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=";
        String withImage=items().replace("\"description\":\"Manutenção\"","\"description\":\"Manutenção\",\"imageData\":\""+image+"\"");
        long quoteId=id(request("POST","/api/quotes",payload(customerId,0,"0",withImage)));
        assertEquals(200,request("PATCH","/api/quotes/"+quoteId+"/status","{\"status\":\"ENVIADO\",\"version\":0}").statusCode());
        assertEquals(200,request("PATCH","/api/quotes/"+quoteId+"/status","{\"status\":\"APROVADO\",\"version\":1}").statusCode());
        var quote=json.readTree(request("GET","/api/quotes/"+quoteId,null).body());
        long itemId=quote.get("items").get(0).get("id").asLong();
        assertEquals(200,request("PATCH","/api/quotes/"+quoteId+"/items/"+itemId+"/status",
            "{\"status\":\"EM_PRODUCAO\",\"version\":2}").statusCode());
        var exports=java.util.Map.of(
            "/api/export/quotes.xlsx","Orçamentos",
            "/api/export/customers.xlsx","Clientes",
            "/api/export/production.xlsx","Itens");
        for(var entry:exports.entrySet()){
            var response=requestBytes(entry.getKey());
            assertEquals(200,response.statusCode(),entry.getKey());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("spreadsheetml"));
            try(var workbook=WorkbookFactory.create(new ByteArrayInputStream(response.body()))){
                assertEquals(entry.getValue(),workbook.getSheetAt(0).getSheetName());
                if(entry.getKey().contains("quotes")||entry.getKey().contains("production"))
                    assertTrue(workbook.getAllPictures().size()>=1,entry.getKey());
            }
        }
    }
    @Test void atualizaStatusIndividualDeItemAprovado() throws Exception {
        long id=create();
        assertEquals(200,request("PATCH","/api/quotes/"+id+"/status","{\"status\":\"ENVIADO\",\"version\":0}").statusCode());
        assertEquals(200,request("PATCH","/api/quotes/"+id+"/status","{\"status\":\"APROVADO\",\"version\":1}").statusCode());
        var quote=json.readTree(request("GET","/api/quotes/"+id,null).body());
        long itemId=quote.get("items").get(0).get("id").asLong();
        var updated=request("PATCH","/api/quotes/"+id+"/items/"+itemId+"/status",
            "{\"status\":\"EM_PRODUCAO\",\"version\":2}");
        assertEquals(200,updated.statusCode(),updated.body());
        var result=json.readTree(updated.body());
        assertEquals("EM_PRODUCAO",result.get("items").get(0).get("status").asText());
        assertEquals(3,result.get("version").asInt());
    }
    @Test void atualizaItensAtomicamente() throws Exception {
        long id=create();var r=request("PUT","/api/quotes/"+id,payload(customerId,0,"0",items()));
        assertEquals(200,r.statusCode(),r.body());assertEquals(1,json.readTree(r.body()).get("version").asInt());
        assertEquals(2,json.readTree(r.body()).get("items").size());
    }
    @Test void rejeitaVersaoAntigaSemPerderDados() throws Exception {
        long id=create();assertEquals(200,request("PUT","/api/quotes/"+id,payload(customerId,0,"0",items())).statusCode());
        assertEquals(409,request("PUT","/api/quotes/"+id,payload(customerId,0,"20",items())).statusCode());
        var q=json.readTree(request("GET","/api/quotes/"+id,null).body());assertEquals(0,q.get("discount").asInt());assertEquals(2,q.get("items").size());
    }
    @Test void rejeitaDescontoMaiorQueSubtotal() throws Exception {
        assertEquals(400,request("POST","/api/quotes",payload(customerId,0,"351",items())).statusCode());
    }
    @Test void rejeitaQuantidadeZero() throws Exception {
        assertEquals(400,request("POST","/api/quotes",payload(customerId,0,"0",items().replace("\"quantity\":2","\"quantity\":0"))).statusCode());
    }
    @Test void rejeitaItensVazios() throws Exception {
        assertEquals(400,request("POST","/api/quotes",payload(customerId,0,"0","[]")).statusCode());
    }
    @Test void rejeitaItemNulo() throws Exception {
        assertEquals(400,request("POST","/api/quotes",payload(customerId,0,"0","[null]")).statusCode());
    }
    @Test void rejeitaPrecoComMaisDeDuasCasas() throws Exception {
        assertEquals(400,request("POST","/api/quotes",payload(customerId,0,"0",items().replace("100.00","100.001"))).statusCode());
    }
    @Test void aceitaNumeroComoStringDoFormulario() throws Exception {
        var body=payload(customerId,0,"\"25.00\"",items().replace("100.00","\"100.00\""));
        assertEquals(201,request("POST","/api/quotes",body).statusCode());
    }
    @Test void rejeitaClienteInexistente() throws Exception {
        assertEquals(404,request("POST","/api/quotes",payload(9999999,0,"0",items())).statusCode());
    }
    @Test void naoPermiteEditarEnviado() throws Exception {
        long id=create();assertEquals(200,request("PATCH","/api/quotes/"+id+"/status","{\"status\":\"ENVIADO\",\"version\":0}").statusCode());
        assertEquals(409,request("PUT","/api/quotes/"+id,payload(customerId,1,"0",items())).statusCode());
    }
    @Test void enviaEAprova() throws Exception {
        long id=create();assertEquals(200,request("PATCH","/api/quotes/"+id+"/status","{\"status\":\"ENVIADO\",\"version\":0}").statusCode());
        var approved=request("PATCH","/api/quotes/"+id+"/status","{\"status\":\"APROVADO\",\"version\":1}");
        assertEquals(200,approved.statusCode());assertEquals("APROVADO",json.readTree(approved.body()).get("status").asText());
    }
    @Test void rejeitaAprovacaoDireta() throws Exception {
        long id=create();assertEquals(409,request("PATCH","/api/quotes/"+id+"/status","{\"status\":\"APROVADO\",\"version\":0}").statusCode());
    }
    @Test void alteraStatusDeVariosOrcamentosAtomicamente() throws Exception {
        long first=create(),second=create();
        var sent=request("PATCH","/api/quotes/status",
            "{\"status\":\"ENVIADO\",\"quotes\":[{\"id\":"+first+",\"version\":0},{\"id\":"+second+",\"version\":0}]}");
        assertEquals(200,sent.statusCode(),sent.body());
        assertEquals(2,json.readTree(sent.body()).size());
        var approved=request("PATCH","/api/quotes/status",
            "{\"status\":\"APROVADO\",\"quotes\":[{\"id\":"+first+",\"version\":1},{\"id\":"+second+",\"version\":1}]}");
        assertEquals(200,approved.statusCode(),approved.body());
        assertEquals("APROVADO",json.readTree(approved.body()).get(0).get("status").asText());
        assertEquals("APROVADO",json.readTree(approved.body()).get(1).get("status").asText());
    }
    @Test void loteComTransicaoInvalidaNaoAlteraNenhumOrcamento() throws Exception {
        long draft=create(),sent=create();
        assertEquals(200,request("PATCH","/api/quotes/"+sent+"/status",
            "{\"status\":\"ENVIADO\",\"version\":0}").statusCode());
        var response=request("PATCH","/api/quotes/status",
            "{\"status\":\"APROVADO\",\"quotes\":[{\"id\":"+draft+",\"version\":0},{\"id\":"+sent+",\"version\":1}]}");
        assertEquals(409,response.statusCode());
        assertEquals("RASCUNHO",json.readTree(request("GET","/api/quotes/"+draft,null).body()).get("status").asText());
        assertEquals("ENVIADO",json.readTree(request("GET","/api/quotes/"+sent,null).body()).get("status").asText());
    }
    @Test void rejeitaEmailInvalido() throws Exception {
        assertEquals(400,request("POST","/api/customers","{\"name\":\"Teste\",\"email\":\"invalido\"}").statusCode());
    }
    @Test void rejeitaClienteSemNome() throws Exception {
        assertEquals(400,request("POST","/api/customers","{\"name\":\"   \"}").statusCode());
    }
    @Test void retorna404ParaOrcamentoInexistente() throws Exception {
        assertEquals(404,request("GET","/api/quotes/99999999",null).statusCode());
    }
    @Test void serveInterface() throws Exception {
        var r=request("GET","/",null);assertEquals(200,r.statusCode());assertTrue(r.body().contains("OrçaFácil"));
    }
}
