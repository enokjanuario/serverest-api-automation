package dev.serverest.factories;

import com.github.javafaker.Faker;
import dev.serverest.models.Produto;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class ProdutoFactory {

    private static final Faker faker = new Faker(new Locale("pt-BR"));

    private ProdutoFactory() {
    }

    public static Produto valido() {
        return Produto.builder()
                .nome("Produto_" + System.nanoTime() + "_" + ThreadLocalRandom.current().nextInt(10000))
                .preco(ThreadLocalRandom.current().nextInt(10, 5000))
                .descricao(faker.lorem().sentence(5))
                .quantidade(ThreadLocalRandom.current().nextInt(1, 500))
                .build();
    }

    public static Produto semCampo(String campo) {
        Produto produto = valido();
        switch (campo.toLowerCase()) {
            case "nome" -> produto.setNome(null);
            case "preco" -> produto.setPreco(null);
            case "descricao" -> produto.setDescricao(null);
            case "quantidade" -> produto.setQuantidade(null);
            default -> throw new IllegalArgumentException("Campo desconhecido: " + campo);
        }
        return produto;
    }
}
