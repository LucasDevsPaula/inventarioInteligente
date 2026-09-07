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

    String tipoFinal = (tipo != null && !tipo.isBlank()) ? tipo.toUpperCase().trim() : "OUTRO";

    String patrimonio = sanitizar(dados.patrimonio());

    String serialOuImei = null;
    if (dados.serviceTagSerial() != null && !dados.serviceTagSerial().isBlank()) {
      serialOuImei = sanitizar(dados.serviceTagSerial());
    } else if (dados.numeroSerie() != null && !dados.numeroSerie().isBlank()) {
      serialOuImei = sanitizar(dados.numeroSerie());
    } else if (dados.imei1() != null && !dados.imei1().isBlank()) {
      serialOuImei = sanitizar(dados.imei1());
    } else if (dados.macAddress() != null && !dados.macAddress().isBlank()) {
      serialOuImei = sanitizar(dados.macAddress());
    }

    StringBuilder nomeBase = new StringBuilder(tipoFinal);
    boolean temPatrimonio = (patrimonio != null && !patrimonio.isBlank());
    boolean temSerial = (serialOuImei != null && !serialOuImei.isBlank());

    if (temPatrimonio && temSerial) {
      nomeBase.append(" - ").append(patrimonio).append(" - ").append(serialOuImei);
    } else if (temPatrimonio) {
      nomeBase.append(" - ").append(patrimonio);
    } else if (temSerial) {
      nomeBase.append(" - ").append(serialOuImei);
    } else {
      nomeBase.append(" - SEM_ID");
    }

    if (totalFotos > 1) {
      nomeBase.append(" - FOTO ").append(indice);
    }
    nomeBase.append(extensao);
    return nomeBase.toString();
  }

  public String sanitizar(String valor) {
    if (valor == null) return null;
    return valor.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
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
