package com.pet.buscaativa.entities.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BuscaAtivaRelatorioDTO {
    private final String nomeCompleto;
    private final String cnsCpf;
    private final LocalDate dataUltimoAtendimento;
    private final Integer faltasConsecutivas;
    private final String tipoAcompanhamento;
    private final String profissionalReferencia;
    private final String telefone;
}