package dev.serverest.factories;

import com.github.javafaker.Faker;
import dev.serverest.models.LoginRequest;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class LoginFactory {

    private static final Faker faker = new Faker(new Locale("pt-BR"));

    private LoginFactory() {
    }

    public static LoginRequest comCredenciais(String email, String password) {
        return LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
    }

    public static LoginRequest invalido() {
        return LoginRequest.builder()
                .email(faker.internet().emailAddress())
                .password(faker.internet().password(8, 16))
                .build();
    }

    public static LoginRequest comEmailInexistente() {
        return LoginRequest.builder()
                .email("inexistente_" + System.nanoTime() + "_" + ThreadLocalRandom.current().nextInt(10000) + "@qa.com")
                .password(faker.internet().password(8, 16))
                .build();
    }

    public static LoginRequest comSenhaErrada(String email) {
        return LoginRequest.builder()
                .email(email)
                .password("senhaErrada" + System.nanoTime())
                .build();
    }

    public static LoginRequest semEmail() {
        return LoginRequest.builder()
                .password(faker.internet().password(8, 16))
                .build();
    }

    public static LoginRequest semPassword() {
        return LoginRequest.builder()
                .email(faker.internet().emailAddress())
                .build();
    }

    public static LoginRequest comEmailInvalido() {
        return LoginRequest.builder()
                .email("email-invalido")
                .password(faker.internet().password(8, 16))
                .build();
    }

    public static LoginRequest vazio() {
        return LoginRequest.builder().build();
    }
}
