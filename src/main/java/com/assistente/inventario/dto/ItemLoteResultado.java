package com.assistente.inventario.dto;

public record ItemLoteResultado(
        boolean ok,
        ItemInventarioResponse payload,
        String erro
) {}
