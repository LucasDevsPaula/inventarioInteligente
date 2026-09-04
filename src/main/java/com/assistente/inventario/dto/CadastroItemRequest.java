package com.assistente.inventario.dto;

import com.assistente.inventario.model.enums.TipoEquipamento;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record CadastroItemRequest(
        String matricula,
        String nome,
        String cargo,
        String setor,
        TipoEquipamento tipoEquipamento,
        List<MultipartFile> fotos
) {}
