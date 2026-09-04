package com.assistente.inventario.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ItemInventarioResponse(
        Long ativoId,
        String matricula,
        String nomeColaborador,
        String cargo,
        String setor,
        String tipoEquipamento,
        String fabricante,
        String modelo,
        String patrimonio,
        String serviceTagSerial,
        String numeroSerie,
        String imeil1,
        String macAddress,
        String linhaCorporativa,
        String processador,
        String status,
        String localizacao,
        List<String> nomesArquivosRenomeados,
        List<String> caminhosFotos,
        LocalDateTime dataRegistro
) {}
