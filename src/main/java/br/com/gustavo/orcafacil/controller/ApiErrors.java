package br.com.gustavo.orcafacil.controller;
import br.com.gustavo.orcafacil.service.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<?> business(BusinessException ex) { return ResponseEntity.status(ex.status()).body(Map.of("message",ex.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        var fields=ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()+": "+e.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(Map.of("message","Revise os campos informados.","errors",fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> invalid(Exception ex) { return ResponseEntity.badRequest().body(Map.of("message","Formato inválido. Confira números, datas e situação.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> integrity(Exception ex) { return ResponseEntity.status(409).body(Map.of("message","Os dados violam uma regra de integridade do banco.")); }
}
