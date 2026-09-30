package com.assistente.inventario.dto;

public record DadosExtraidosFotoDto(
        String equipamento,
        String fabricante,
        String modelo,
        String patrimonio,
        String serviceTagSerial,
        String numeroSerie,
        String imei1,
        String macAddress,
        String linhaCorporativa,
        String processador,
        String localizacao
) {}
