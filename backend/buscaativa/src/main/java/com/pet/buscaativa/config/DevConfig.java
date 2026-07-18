package com.pet.buscaativa.config;

import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.enums.TipoUsuario;
import com.pet.buscaativa.entities.enums.UnidadeAtuacao;
import com.pet.buscaativa.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DevConfig implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setEmail("admin@pet.com");
        admin.setSenha(passwordEncoder.encode("123456"));
        admin.setTipoUsuario(TipoUsuario.ADMINISTRADOR);
        admin.setUnidadeAtuacao(UnidadeAtuacao.USF);

        Usuario medico = new Usuario();
        medico.setNome("João Ferreira");
        medico.setEmail("profissional@pet.com");
        medico.setSenha(passwordEncoder.encode("123456"));
        medico.setTipoUsuario(TipoUsuario.PROFISSIONAL);
        medico.setUnidadeAtuacao(UnidadeAtuacao.CAPS_AD);

        Usuario recepcao = new Usuario();
        recepcao.setNome("Ana de Jesus");
        recepcao.setEmail("recepcao@pet.com");
        recepcao.setSenha(passwordEncoder.encode("123456"));
        recepcao.setTipoUsuario(TipoUsuario.RECEPCAO);
        recepcao.setUnidadeAtuacao(UnidadeAtuacao.CAPS_II);

        usuarioRepository.saveAll(
                Arrays.asList(admin, medico, recepcao)
        );
    }

}