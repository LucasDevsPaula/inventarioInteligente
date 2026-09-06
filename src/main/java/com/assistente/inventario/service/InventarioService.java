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

  public byte[] exportarExcel() throws IOException {
    List<Ativo> ativos = ativoRepository.findAll();

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
      for (Ativo ativo : ativos) {
        Row row = sheet.createRow(rowIdx++);

        Colaborador colaborador = ativo.getColaborador();

        row.createCell(0).setCellValue(ativo.getId() != null ? ativo.getId().toString() : "");
        row.createCell(1)
            .setCellValue(
                colaborador != null && colaborador.getMatricula() != null
                    ? colaborador.getMatricula()
                    : "");
        row.createCell(2)
            .setCellValue(
                colaborador != null && colaborador.getNome() != null ? colaborador.getNome() : "");
        row.createCell(3)
            .setCellValue(
                colaborador != null && colaborador.getCargo() != null
                    ? colaborador.getCargo()
                    : "");
        row.createCell(4)
            .setCellValue(
                colaborador != null && colaborador.getSetor() != null
                    ? colaborador.getSetor()
                    : "");
        row.createCell(5)
            .setCellValue(
                ativo.getTipoEquipamento() != null ? ativo.getTipoEquipamento().name() : "");
        row.createCell(6).setCellValue(ativo.getFabricante() != null ? ativo.getFabricante() : "");
        row.createCell(7).setCellValue(ativo.getModelo() != null ? ativo.getModelo() : "");
        row.createCell(8).setCellValue(ativo.getPatrimonio() != null ? ativo.getPatrimonio() : "");
        row.createCell(9)
            .setCellValue(ativo.getServiceTagSerial() != null ? ativo.getServiceTagSerial() : "");
        row.createCell(10)
            .setCellValue(ativo.getNumeroSerie() != null ? ativo.getNumeroSerie() : "");
        row.createCell(11).setCellValue(ativo.getImei1() != null ? ativo.getImei1() : "");
        row.createCell(12).setCellValue(ativo.getMacAddress() != null ? ativo.getMacAddress() : "");
        row.createCell(13)
            .setCellValue(ativo.getLinhaCorporativa() != null ? ativo.getLinhaCorporativa() : "");
        row.createCell(14)
            .setCellValue(ativo.getProcessador() != null ? ativo.getProcessador() : "");
        row.createCell(15).setCellValue(ativo.getStatus() != null ? ativo.getStatus() : "");

        String localizacao = "";
        List<String> fotosNomes = new ArrayList<>();

        if (ativo.getFotos() != null && !ativo.getFotos().isEmpty()) {
          for (RegistroInventario registro : ativo.getFotos()) {
            if (registro.getNomeArquivoRenomeado() != null) {
              fotosNomes.add(registro.getNomeArquivoRenomeado());
            }
            if (registro.getLocalizacao() != null && localizacao.isEmpty()) {
              localizacao = registro.getLocalizacao();
            }
          }
        }

        row.createCell(16).setCellValue(localizacao);

        Cell cellFotos = row.createCell(17);
        cellFotos.setCellValue(String.join(", ", fotosNomes));
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
