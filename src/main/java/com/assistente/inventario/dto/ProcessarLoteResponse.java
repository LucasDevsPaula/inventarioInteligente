package com.assistente.inventario.dto;

import java.util.List;

public record ProcessarLoteResponse(
        int total,
        int sucesso,
        int falha,
        List<ItemLoteResultado> itens
) {}
