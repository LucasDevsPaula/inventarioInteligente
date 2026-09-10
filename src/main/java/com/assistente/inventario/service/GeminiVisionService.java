package com.assistente.inventario.service;

import com.assistente.inventario.dto.DadosExtraidosFotoDto;
import com.assistente.inventario.model.enums.TipoEquipamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class GeminiVisionService {
  private final Client client;
  private final ObjectMapper objectMapper;

  public GeminiVisionService(@Value("${gemini.api.key}") String apikey) {
    this.client = Client.builder().apiKey(apikey).build();
    this.objectMapper = new ObjectMapper();
  }

  public DadosExtraidosFotoDto extrairDados(
      List<MultipartFile> files, TipoEquipamento tipoInformado) {
    try {
      if (files == null || files.isEmpty()) {
        throw new IllegalArgumentException("Nenhuma imagem foi fornecida.");
      }

      String tipoHint = (tipoInformado != null) ? tipoInformado.name() : "NÃO ESPECIFICADO";

      String prompt =
          """
                Você é um especialista em inventário de ativos de TI.
                O usuário indicou que o tipo do equipamento é: %s.

                Analise a imagem da etiqueta/tela e extraia as seguintes informações:
                1. "equipamento": DESKTOP, MONITOR, SMARTPHONE, NOTEBOOK, etc.
                2. "fabricante": Dell, HP, Motorola, Samsung, Lenovo, Apple, etc.
                3. "modelo": Nome ou código do modelo comercial (se celular, identifique via TAC do IMEI; se Dell, pelo padrão do gabinete/etiqueta).
                4. "patrimonio": Número do patrimônio impresso sob código de barras ou QR code (apenas dígitos). Se não tiver, retorne null.
                5. "serviceTagSerial": Se Dell, a Service Tag (ex: 4VP7TY3). Se outro fabricante, o Serial Number principal.
                6. "numeroSerie": Se Dell, o Express Service Code (ex: 10623936603) e em monitor e switch (ex: S/N: BR-067NW4-TVB00-310-2S6L-A00). Se outro fabricante, o S/N secundário.
                7. "imei1": Se for smartphone, o código IMEI 1 (ex: 352790349102699).
                8. "macAddress": Endereço MAC de rede se estiver visível na etiqueta.
                9. "linhaCorporativa": Número de telefone/chip corporativo se visível na tela (ex: Vivo - (11) 91450-9855).
                10. "processador": Processador se visível na etiqueta ou característico do modelo.
                11. "localizacao": Endereço ou aeroporto/cidade presente no carimbo de GPS da foto.

                Retorne EXCLUSIVAMENTE um JSON válido com esses campos (use null para o que não encontrar), sem markdown extra.
                """
              .formatted(tipoHint);

      List<Part> partes = new ArrayList<>();

      partes.add(Part.builder().text(prompt).build());

      for (MultipartFile file : files) {
        if (file != null && !file.isEmpty()) {
          String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

          String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
          Part fotoPart =
              Part.builder()
                  .inlineData(Blob.builder().data(base64Data).mimeType(mimeType).build())
                  .build();

          partes.add(fotoPart);
        }
      }

      Content content = Content.builder().parts(partes).build();

      GenerateContentResponse response =
          client.models.generateContent("gemini-3.6-flash", content, null);

      String rawJson = response.text().replaceAll("```json", "").replaceAll("```", "").trim();

      log.info("JSON extraído pela IA: {}", rawJson);
      return objectMapper.readValue(rawJson, DadosExtraidosFotoDto.class);

    } catch (Exception e) {
      log.error("Erro ao extrair dados da imegem com o Gemini: {}", e.getMessage(), e);
      throw new RuntimeException("Falha no processamento visual da imagem: " + e.getMessage(), e);
    }
  }
}
