package com.assistente.inventario.service;

import com.assistente.inventario.dto.CadastroItemRequest;
import com.assistente.inventario.dto.CadastroLoteRequest;
import com.assistente.inventario.dto.DadosExtraidosFotoDto;
import com.assistente.inventario.dto.ItemInventarioResponse;
import com.assistente.inventario.model.Ativo;
import com.assistente.inventario.model.Colaborador;
import com.assistente.inventario.model.RegistroInventario;
import com.assistente.inventario.model.enums.TipoEquipamento;
import com.assistente.inventario.repository.AtivoRepository;
import com.assistente.inventario.repository.ColaboradorRepository;
import com.assistente.inventario.repository.RegistroInventarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioService {

  private final ColaboradorRepository colaboradorRepository;
  private final AtivoRepository ativoRepository;
  private final RegistroInventarioRepository registroInventarioRepository;
  private final GeminiVisionService geminiVisionService;
  private final FotoStorageService fotoStorageService;

  @Transactional
  public ItemInventarioResponse processarItem(CadastroItemRequest request) throws IOException {
    if (request.fotos() == null || request.fotos().isEmpty()) {
      throw new IllegalArgumentException("É necessário enviar pelo menos uma foto do ativo.");
    }

    // Extração visual multimodal com IA (1 ou mais fotos)
    DadosExtraidosFotoDto dadosIa =
        geminiVisionService.extrairDados(request.fotos(), request.tipoEquipamento());

    TipoEquipamento tipoFinal = request.tipoEquipamento();
    if (tipoFinal == null && dadosIa.equipamento() != null) {
      try {
        tipoFinal = TipoEquipamento.valueOf(dadosIa.equipamento().toUpperCase().trim());
      } catch (IllegalArgumentException e) {
        tipoFinal = TipoEquipamento.OUTRO;
      }
    }

    if (tipoFinal == null) {
      tipoFinal = TipoEquipamento.OUTRO;
    }

    // Buscar ou atualizar colaborador
    Colaborador colaborador =
        colaboradorRepository
            .findByMatricula(request.matricula())
            .map(
                existente -> {
                  existente.setNome(request.nome());
                  existente.setCargo(request.cargo());
                  existente.setSetor(request.setor());
                  return colaboradorRepository.save(existente);
                })
            .orElseGet(
                () ->
                    colaboradorRepository.save(
                        Colaborador.builder()
                            .matricula(request.matricula())
                            .nome(request.nome())
                            .cargo(request.cargo())
                            .setor(request.setor())
                            .build()));

    Optional<Ativo> ativoExistente =
        ativoRepository.buscarExistente(
            dadosIa.patrimonio(), dadosIa.serviceTagSerial(), dadosIa.imeil1());

    Ativo ativo = ativoExistente.orElseGet(Ativo::new);
    ativo.setColaborador(colaborador);
    ativo.setTipoEquipamento(tipoFinal);
    ativo.setFabricante(dadosIa.fabricante());
    ativo.setModelo(dadosIa.modelo());
    ativo.setPatrimonio(dadosIa.patrimonio());
    ativo.setServiceTagSerial(dadosIa.serviceTagSerial());
    ativo.setNumeroSerie(dadosIa.numeroSerie());
    ativo.setImei1(dadosIa.imeil1());
    ativo.setMacAddress(dadosIa.macAddress());
    ativo.setProcessador(dadosIa.processador());
    ativo.setStatus("EM_USO");

    Ativo ativoSalvo = ativoRepository.save(ativo);

    // Salvar e renomear fotos
    int totalFotos = request.fotos().size();
    List<String> nomesRenomeados = new ArrayList<>();
    List<String> caminhosSalvos = new ArrayList<>();

    for (int i = 0; i < totalFotos; i++) {
      MultipartFile foto = request.fotos().get(i);
      if (foto == null || foto.isEmpty()) continue;

      String nomeOriginal = foto.getOriginalFilename();
      String nomeRenomeado =
          fotoStorageService.gerarNomePadrao(
              tipoFinal.name(), dadosIa, nomeOriginal, i + 1, totalFotos);

      Path caminhoSalvo = fotoStorageService.salvarFoto(foto, nomeRenomeado);
      nomesRenomeados.add(nomeRenomeado);
      caminhosSalvos.add(caminhoSalvo.toString());

      RegistroInventario registro =
          RegistroInventario.builder()
              .ativo(ativoSalvo)
              .nomeArquivoOriginal(nomeOriginal)
              .nomeArquivoRenomeado(nomeRenomeado)
              .caminhoFoto(caminhoSalvo.toString())
              .build();

      registroInventarioRepository.save(registro);
    }

    return new ItemInventarioResponse(
        ativoSalvo.getId(),
        colaborador.getMatricula(),
        colaborador.getNome(),
        colaborador.getCargo(),
        colaborador.getSetor(),
        ativoSalvo.getTipoEquipamento().name(),
        ativoSalvo.getFabricante(),
        ativoSalvo.getModelo(),
        ativoSalvo.getPatrimonio(),
        ativoSalvo.getServiceTagSerial(),
        ativoSalvo.getNumeroSerie(),
        ativoSalvo.getImei1(),
        ativoSalvo.getMacAddress(),
        ativoSalvo.getLinhaCorporativa(),
        ativoSalvo.getProcessador(),
        ativoSalvo.getStatus(),
        dadosIa.localizacao(),
        nomesRenomeados,
        caminhosSalvos,
        LocalDateTime.now());
  }

  public List<ItemInventarioResponse> processarLote(CadastroLoteRequest request) {
    List<ItemInventarioResponse> resultados = new ArrayList<>();
    if (request.fotos() == null) return resultados;

    for (MultipartFile foto : request.fotos()) {
      if (foto == null || foto.isEmpty()) continue;
      try {
        // Em lote cada item será independente
        CadastroItemRequest itemRequest =
            new CadastroItemRequest(
                request.matricula(),
                request.nome(),
                request.cargo(),
                request.setor(),
                request.tipoEquipamento(),
                List.of(foto));
        resultados.add(processarItem(itemRequest));
      } catch (Exception e) {
        log.error("Erro ao processar foto individual do lote: {}", foto.getOriginalFilename(), e);
      }
    }
    return resultados;
  }

  public byte[] exportarExcel(List<ItemInventarioResponse> itens) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Inventário T3 SBCR");

      CellStyle headerStyle = workbook.createCellStyle();
      Font font = workbook.createFont();
      font.setBold(true);
      font.setColor(IndexedColors.WHITE.getIndex());
      headerStyle.setFont(font);
      headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      CellStyle wrapStyle = workbook.createCellStyle();
      wrapStyle.setWrapText(true);

      String[] colunas = {
        "MATRÍCULA",
        "NOME",
        "CARGO",
        "SETOR",
        "EQUIPAMENTO",
        "FABRICANTE",
        "MODELO",
        "PATRIMÔNIO",
        "SERVICE TAG / S/N",
        "NÚMERO DE SÉRIE",
        "IMEI 1",
        "MAC ADDRESS",
        "LINHA CORPORATIVA",
        "PROCESSADOR",
        "LOCALIZAÇÃO",
        "FOTOS RENOMEADAS"
      };

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < colunas.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(colunas[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowIdx = 1;
      for (ItemInventarioResponse item : itens) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(item.matricula() != null ? item.matricula() : "");
        row.createCell(1)
            .setCellValue(item.nomeColaborador() != null ? item.nomeColaborador() : "");
        row.createCell(2).setCellValue(item.cargo() != null ? item.cargo() : "");
        row.createCell(3).setCellValue(item.setor() != null ? item.setor() : "");
        row.createCell(4)
            .setCellValue(item.tipoEquipamento() != null ? item.tipoEquipamento() : "");
        row.createCell(5).setCellValue(item.fabricante() != null ? item.fabricante() : "");
        row.createCell(6).setCellValue(item.modelo() != null ? item.modelo() : "");
        row.createCell(7).setCellValue(item.patrimonio() != null ? item.patrimonio() : "");
        row.createCell(8)
            .setCellValue(item.serviceTagSerial() != null ? item.serviceTagSerial() : "");
        row.createCell(9).setCellValue(item.numeroSerie() != null ? item.numeroSerie() : "");
        row.createCell(10).setCellValue(item.imeil1() != null ? item.imeil1() : "");
        row.createCell(11).setCellValue(item.macAddress() != null ? item.macAddress() : "");
        row.createCell(12)
            .setCellValue(item.linhaCorporativa() != null ? item.linhaCorporativa() : "");
        row.createCell(13).setCellValue(item.processador() != null ? item.processador() : "");
        row.createCell(14).setCellValue(item.localizacao() != null ? item.localizacao() : "");

        String fotosConcatenadas =
            item.nomesArquivosRenomeados() != null
                ? String.join(", ", item.nomesArquivosRenomeados())
                : "";
        Cell cellFotos = row.createCell(15);
        cellFotos.setCellValue(fotosConcatenadas);
        cellFotos.setCellStyle(wrapStyle);
      }

      for (int i = 0; i < colunas.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    }
  }
}
