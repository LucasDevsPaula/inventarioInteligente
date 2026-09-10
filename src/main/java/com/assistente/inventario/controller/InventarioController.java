package com.assistente.inventario.controller;

import com.assistente.inventario.dto.CadastroItemRequest;
import com.assistente.inventario.dto.CadastroLoteRequest;
import com.assistente.inventario.dto.ItemInventarioResponse;
import com.assistente.inventario.dto.ProcessarLoteResponse;
import com.assistente.inventario.service.FotoStorageService;
import com.assistente.inventario.service.InventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventarioController {

  private final InventarioService inventarioService;
  private final FotoStorageService fotoStorageService;

  @PostMapping(value = "/processar-item", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ItemInventarioResponse> processarItem(
      @ModelAttribute CadastroItemRequest request) throws IOException {
    return ResponseEntity.ok(inventarioService.processarItem(request));
  }

  @PostMapping(value = "/processar-lotes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProcessarLoteResponse> processarLotes(
      @ModelAttribute CadastroLoteRequest request) {
    ProcessarLoteResponse response =  inventarioService.processarLote(request);
    if(response.falha() == 0){
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.status(207).body(response);
  }

  @GetMapping("/exportar-excel")
  public ResponseEntity<byte[]> exportarExcel() throws IOException {
    byte[] excelBytes = inventarioService.exportarExcel();

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventario_t3_sbcr.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(excelBytes);
  }

  @GetMapping("/fotos/{nomeArquivo}")
  public ResponseEntity<Resource> exibirFoto(@PathVariable String nomeArquivo) {
    try {
      Path caminhoFoto = fotoStorageService.getDiretorioDestino().resolve(nomeArquivo).normalize();
      Resource resource = new UrlResource(caminhoFoto.toUri());

      if (resource.exists() && resource.isReadable()) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource);
      }
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Erro ao carregar a foto {}: {}", nomeArquivo, e.getMessage());
      return ResponseEntity.internalServerError().build();
    }
  }
}
