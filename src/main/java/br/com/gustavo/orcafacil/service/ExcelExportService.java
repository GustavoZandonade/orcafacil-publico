package br.com.gustavo.orcafacil.service;

import br.com.gustavo.orcafacil.model.Quote;
import br.com.gustavo.orcafacil.model.QuoteItem;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {
    private static final int WEBP_CHUNK_SIZE = 30_000;
    private final QuoteService quoteService;

    public ExcelExportService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    public byte[] quotes() {
        var quotes = quoteService.all();
        return workbook(workbook -> {
            var sheet = workbook.createSheet("Orçamentos");
            var headers = List.of("Nº", "Título", "Cliente", "Validade", "Situação", "Descrição",
                "Subtotal (R$)", "Desconto (R$)", "Total (R$)", "Quantidade de itens");
            header(sheet, headers);
            int rowIndex = 1;
            for (var quote : quotes) {
                var row = sheet.createRow(rowIndex++);
                cell(row, 0, quote.id());
                cell(row, 1, quote.title());
                cell(row, 2, quote.customerName());
                cell(row, 3, quote.validUntil().toString());
                cell(row, 4, quote.status().name());
                cell(row, 5, quote.description());
                cell(row, 6, quote.subtotal());
                cell(row, 7, quote.discount());
                cell(row, 8, quote.total());
                cell(row, 9, quote.items().size());
            }
            fit(sheet, headers.size());
            writeItems(workbook, quotes.stream()
                .flatMap(quote -> quote.items().stream().map(item -> new QuoteItemRow(quote, item))).toList());
        });
    }

    public byte[] customers() {
        var customers = quoteService.customers();
        return workbook(workbook -> {
            var sheet = workbook.createSheet("Clientes");
            var headers = List.of("Nº", "Responsável / Empresa", "CNPJ", "Endereço", "E-mail");
            header(sheet, headers);
            int rowIndex = 1;
            for (var customer : customers) {
                var row = sheet.createRow(rowIndex++);
                cell(row, 0, customer.id());
                cell(row, 1, customer.displayName());
                cell(row, 2, customer.cnpj());
                cell(row, 3, customer.address());
                cell(row, 4, customer.email());
            }
            fit(sheet, headers.size());
        });
    }

    public byte[] production() {
        var items = quoteService.all().stream()
            .filter(quote -> quote.status().name().equals("APROVADO"))
            .flatMap(quote -> quote.items().stream()
                .filter(item -> item.status().name().equals("EM_PRODUCAO")
                    || item.status().name().equals("CONCLUIDO"))
                .map(item -> new QuoteItemRow(quote, item)))
            .toList();
        return workbook(workbook -> writeItems(workbook, items));
    }

    private void writeItems(Workbook workbook, List<QuoteItemRow> items) {
        var sheet = workbook.createSheet("Itens");
        var headers = List.of("Nº orçamento", "Orçamento", "Cliente", "Item", "Quantidade",
            "Valor unitário (R$)", "Subtotal (R$)", "Situação do item", "Imagem");
        header(sheet, headers);
        var drawing = sheet.createDrawingPatriarch();
        var webpImages = new ArrayList<WebpImage>();
        int rowIndex = 1;
        for (var entry : items) {
            var quote = entry.quote();
            var item = entry.item();
            var row = sheet.createRow(rowIndex);
            cell(row, 0, quote.id());
            cell(row, 1, quote.title());
            cell(row, 2, quote.customerName());
            cell(row, 3, item.description());
            cell(row, 4, item.quantity());
            cell(row, 5, item.unitPrice());
            cell(row, 6, item.lineTotal());
            cell(row, 7, item.status().name());
            String imageDescription = addImage(workbook, drawing, sheet, row, rowIndex, item);
            cell(row, 8, imageDescription);
            if (item.imageData() != null && item.imageData().startsWith("data:image/webp;base64,")) {
                webpImages.add(new WebpImage(item.id(), item.imageData().substring(item.imageData().indexOf(',') + 1)));
            }
            rowIndex++;
        }
        fit(sheet, headers.size());
        if (!webpImages.isEmpty()) writeWebpData(workbook, webpImages);
    }

    private String addImage(Workbook workbook, org.apache.poi.ss.usermodel.Drawing<?> drawing,
        Sheet sheet, Row row, int rowIndex, QuoteItem item) {
        if (item.imageData() == null || item.imageData().isBlank()) return "";
        int separator = item.imageData().indexOf(',');
        if (separator < 0) return "Imagem indisponível";
        String header = item.imageData().substring(0, separator);
        int pictureType;
        if (header.equals("data:image/png;base64")) pictureType = Workbook.PICTURE_TYPE_PNG;
        else if (header.equals("data:image/jpeg;base64")) pictureType = Workbook.PICTURE_TYPE_JPEG;
        else if (header.equals("data:image/webp;base64")) return "Dados disponíveis na aba Imagens WebP";
        else return "Formato de imagem não reconhecido";
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(item.imageData().substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            return "Imagem inválida";
        }
        int pictureId = workbook.addPicture(imageBytes, pictureType);
        ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setCol1(8);
        anchor.setCol2(9);
        anchor.setRow1(rowIndex);
        anchor.setRow2(rowIndex + 1);
        drawing.createPicture(anchor, pictureId);
        sheet.setColumnWidth(8, 20 * 256);
        row.setHeightInPoints(60);
        return "Imagem incorporada";
    }

    private void writeWebpData(Workbook workbook, List<WebpImage> images) {
        var sheet = workbook.createSheet("Imagens WebP (base64)");
        var headers = List.of("Nº do item", "Parte", "Total de partes", "Dados base64");
        header(sheet, headers);
        int rowIndex = 1;
        for (var image : images) {
            int parts = (image.data().length() + WEBP_CHUNK_SIZE - 1) / WEBP_CHUNK_SIZE;
            for (int part = 0; part < parts; part++) {
                int start = part * WEBP_CHUNK_SIZE;
                var row = sheet.createRow(rowIndex++);
                cell(row, 0, image.itemId());
                cell(row, 1, part + 1);
                cell(row, 2, parts);
                cell(row, 3, image.data().substring(start, Math.min(start + WEBP_CHUNK_SIZE, image.data().length())));
            }
        }
        fit(sheet, headers.size());
        sheet.setColumnWidth(3, 80 * 256);
    }

    private byte[] workbook(WorkbookWriter writer) {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            writer.write(workbook);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível gerar o arquivo Excel.", ex);
        }
    }

    private void header(Sheet sheet, List<String> headers) {
        var row = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) cell(row, i, headers.get(i));
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.size() - 1));
    }

    private void fit(Sheet sheet, int columns) {
        for (int column = 0; column < columns; column++) {
            if (column != 8) sheet.autoSizeColumn(column);
            sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column), 60 * 256));
        }
    }

    private void cell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private void cell(Row row, int column, long value) {
        row.createCell(column).setCellValue(value);
    }

    private void cell(Row row, int column, int value) {
        row.createCell(column).setCellValue(value);
    }

    private void cell(Row row, int column, BigDecimal value) {
        row.createCell(column).setCellValue(value == null ? 0 : value.doubleValue());
    }

    private record QuoteItemRow(Quote quote, QuoteItem item) { }
    private record WebpImage(Long itemId, String data) { }
    @FunctionalInterface private interface WorkbookWriter { void write(Workbook workbook) throws IOException; }
}
