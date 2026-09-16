package com.pet.buscaativa.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.pet.buscaativa.services.exceptions.RelatorioException;

@Service
public class PdfRendererService {
    private static final String CSS = "reports/relatorios-pdf.css";
    private static final String IMAGENS = "reports/images/";

    private final TemplateEngine templateEngine;
    private final String css;

    public PdfRendererService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.css = lerTexto(CSS);
    }

    public byte[] renderizar(String template, Map<String, Object> variaveis) {
        Context contexto = new Context();
        contexto.setVariables(variaveis);
        contexto.setVariable("css", css);
        contexto.setVariable("logo", imagem("logo-sibraps.png"));
        contexto.setVariable("iconeCalendario", imagem("icon-calendar.png"));
        contexto.setVariable("iconeUsuario", imagem("icon-user.png"));
        contexto.setVariable("iconeGrupo", imagem("icon-group.png"));
        contexto.setVariable("iconeSessoes", imagem("icon-sessions.png"));
        contexto.setVariable("iconeGrafico", imagem("icon-chart.png"));
        contexto.setVariable("iconeAlerta", imagem("icon-alert.png"));
        contexto.setVariable("iconeInfo", imagem("icon-info.png"));

        String html = templateEngine.process("reports/" + template, contexto);
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(saida);
            builder.run();
            return saida.toByteArray();
        } catch (Exception e) {
            throw new RelatorioException("Não foi possível gerar o relatório em PDF.", e);
        }
    }

    private String imagem(String nome) {
        try (InputStream recurso = new ClassPathResource(IMAGENS + nome).getInputStream()) {
            byte[] bytes = recurso.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RelatorioException("Não foi possível carregar um recurso visual do relatório.", e);
        }
    }

    private String lerTexto(String caminho) {
        try (InputStream recurso = new ClassPathResource(caminho).getInputStream()) {
            return new String(recurso.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RelatorioException("Não foi possível carregar o estilo dos relatórios.", e);
        }
    }
}