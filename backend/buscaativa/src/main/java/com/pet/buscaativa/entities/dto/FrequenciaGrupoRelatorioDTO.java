package com.pet.buscaativa.entities.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FrequenciaGrupoRelatorioDTO {
    private final LocalDate data;
    private final String grupo;
    private final String coordenador;
    private final Integer quantidadePresentes;
    private final Integer quantidadeAusentes;
    private final List<String> nomesPresentes;
    private final List<String> nomesAusentes;
    private final BigDecimal taxaPresenca;
    private final String taxaPresencaFormatada;
}