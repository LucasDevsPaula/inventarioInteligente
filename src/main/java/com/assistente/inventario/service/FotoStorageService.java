package com.assistente.inventario.service;

import com.assistente.inventario.dto.DadosExtraidosFotoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class FotoStorageService {
  private final Path diretorioDestino;

  public FotoStorageService(
      @Value("${app.storage.diretorio:./fotos_inventario}") String diretorio) {
    this.diretorioDestino = Paths.get(diretorio).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.diretorioDestino);
    } catch (IOException e) {
      throw new RuntimeException(
          "Não foi possível criar o diretório de fotos: " + diretorioDestino, e);
    }
  }

  public String gerarNomePadrao(
      String tipo, DadosExtraidosFotoDto dados, String nomeOriginal, int indice, int totalFotos) {
    String extensao =
        (nomeOriginal != null && nomeOriginal.contains("."))
            ? nomeOriginal.substring(nomeOriginal.lastIndexOf("."))
            : ".jpg";

    String identificador = "SEM_ID";
    if (dados.patrimonio() != null && !dados.patrimonio().isBlank()) {
      identificador = dados.patrimonio().trim();
    } else if (dados.imei1() != null && !dados.imei1().isBlank()) {
      identificador = dados.imei1().trim();
    } else if (dados.serviceTagSerial() != null && !dados.serviceTagSerial().isBlank()) {
      identificador = dados.serviceTagSerial().trim();
    }

    identificador = identificador.replaceAll("[\\\\/:*?\"<>|]", "_");
    String tipoFinal = (tipo != null && !tipo.isBlank()) ? tipo.toUpperCase().trim() : "OUTRO";

    if (totalFotos > 1) {
      return String.format("%s - %s - FOTO %d%s", tipoFinal, identificador, totalFotos, extensao);
    }
    return String.format("%s - %s%s", tipoFinal, identificador, extensao);
  }

  public Path salvarFoto(MultipartFile file, String nomeFinal) throws IOException {
    Path destino = this.diretorioDestino.resolve(nomeFinal).normalize();
    Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
    log.info("Foto salva com sucesso em: {}", destino);
    return destino;
  }

  public Path getDiretorioDestino() {
    return this.diretorioDestino;
  }
}
