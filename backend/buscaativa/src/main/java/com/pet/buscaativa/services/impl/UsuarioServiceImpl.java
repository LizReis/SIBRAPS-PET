package com.pet.buscaativa.services.impl;

import java.util.List;
import java.util.UUID;

import com.pet.buscaativa.entities.Usuario;
import com.pet.buscaativa.entities.dto.UsuarioDTO;
import com.pet.buscaativa.entities.enums.TipoUsuario;
import com.pet.buscaativa.mapping.UsuarioMapper;
import com.pet.buscaativa.repositories.UsuarioRepository;
import com.pet.buscaativa.services.UsuarioService;
import com.pet.buscaativa.services.exceptions.DatabaseException;
import com.pet.buscaativa.services.exceptions.RecursoDuplicadoException;
import com.pet.buscaativa.services.exceptions.ResourceNotFoundException;

import com.pet.buscaativa.services.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UsuarioDTO save(UsuarioDTO usuarioDTO) {
        validarEmailDuplicado(usuarioDTO.email(), usuarioDTO.idPublico());

        // Senha é obrigatória apenas no cadastro (idPublico == null)
        boolean isCadastro = usuarioDTO.idPublico() == null;

        if (isCadastro && (usuarioDTO.senha() == null || usuarioDTO.senha().isBlank())) {
            throw new ValidationException("O campo de senha não pode ficar vazio.");
        }

        Usuario usuarioSalvar;

        if (usuarioDTO.idPublico() != null) {
            usuarioSalvar = usuarioRepository.findByIdPublico(usuarioDTO.idPublico())
                    .orElseThrow(() -> new DatabaseException("Usuário não encontrado!"));

            usuarioSalvar.setNome(usuarioDTO.nome());
            usuarioSalvar.setEmail(usuarioDTO.email());
            usuarioSalvar.setTipoUsuario(usuarioDTO.tipoUsuario());
            usuarioSalvar.setUnidadeAtuacao(usuarioDTO.unidadeAtuacao());

        } else {
            usuarioSalvar = usuarioMapper.toUsuarioEntity(usuarioDTO);

            if (usuarioRepository.count() == 0) {
                usuarioSalvar.setTipoUsuario(TipoUsuario.ADMINISTRADOR);
            }
        }

        if (usuarioDTO.senha() != null && !usuarioDTO.senha().isBlank()) {
            usuarioSalvar.setSenha(passwordEncoder.encode(usuarioDTO.senha()));
        }

        usuarioSalvar = usuarioRepository.save(usuarioSalvar);

        return usuarioMapper.toUsuarioDTO(usuarioSalvar);
    }

    @Override
    public List<UsuarioDTO> findAll() {
        List<Usuario> list = usuarioRepository.findAll();
        return list.stream()
                .map(UsuarioDTO::new)
                .toList();
    }

    @Override
    public UsuarioDTO findById(UUID idPublic) {
        Usuario usuario = usuarioRepository.findByIdPublico(idPublic)
                .orElseThrow(() -> new ResourceNotFoundException(idPublic));
        return new UsuarioDTO(usuario);
    }

    public void validarEmailDuplicado(String email, UUID idPublic) {
        usuarioRepository.findByEmail(email).ifPresent(
                usuario -> {
                    // Se o email já existe, mas pertence a um usuário DIFERENTE, lança exceção
                    if (!usuario.getIdPublico().equals(idPublic)) {
                        throw new RecursoDuplicadoException("E-mail já cadastrado no sistema!");
                    }
                    // Se pertence ao mesmo usuário (idPublic é igual), permite continuar
                });
    }

    @Override
    public UsuarioDTO findByEmail(String email) {
        var usuarioEntity = usuarioRepository.findByEmail(email).orElse(null);
        if (usuarioEntity == null) {
            return null;
        }

        return usuarioMapper.toUsuarioDTO(usuarioEntity);
    }

    @Override
    public void removerUsuario(UUID idPublico) {
        var usuarioEntity = usuarioRepository.findByIdPublico(idPublico).orElse(null);
        if (usuarioEntity == null) {
            throw new DatabaseException("Usuário não encontrado para remoção.");
        } else {
            usuarioRepository.deleteById(usuarioEntity.getId());
        }

    }

}
