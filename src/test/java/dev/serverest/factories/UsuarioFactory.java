package dev.serverest.factories;

import com.github.javafaker.Faker;
import dev.serverest.models.Usuario;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class UsuarioFactory {

    private static final Faker faker = new Faker(new Locale("pt-BR"));

    private UsuarioFactory() {
    }

    private static String gerarEmail() {
        return "faker_" + System.nanoTime() + "_" + ThreadLocalRandom.current().nextInt(10000) + "@qa.com";
    }

    public static Usuario valido() {
        return Usuario.builder()
                .nome(faker.name().fullName())
                .email(gerarEmail())
                .password(faker.internet().password(8, 16))
                .administrador("true")
                .build();
    }

    public static Usuario naoAdmin() {
        return Usuario.builder()
                .nome(faker.name().fullName())
                .email(gerarEmail())
                .password(faker.internet().password(8, 16))
                .administrador("false")
                .build();
    }

    public static Usuario comEmailEspecifico(String email) {
        Usuario usuario = valido();
        usuario.setEmail(email);
        return usuario;
    }

    public static Usuario semCampo(String campo) {
        Usuario usuario = valido();
        switch (campo.toLowerCase()) {
            case "nome" -> usuario.setNome(null);
            case "email" -> usuario.setEmail(null);
            case "password" -> usuario.setPassword(null);
            case "administrador" -> usuario.setAdministrador(null);
            default -> throw new IllegalArgumentException("Campo desconhecido: " + campo);
        }
        return usuario;
    }

    public static Usuario comNome(String nome) {
        Usuario usuario = valido();
        usuario.setNome(nome);
        return usuario;
    }
}
