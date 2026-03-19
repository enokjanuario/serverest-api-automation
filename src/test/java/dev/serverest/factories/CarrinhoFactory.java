package dev.serverest.factories;

import dev.serverest.models.Carrinho;

import java.util.List;

public final class CarrinhoFactory {

    private CarrinhoFactory() {
    }

    public static Carrinho comProduto(String idProduto, int quantidade) {
        return Carrinho.builder()
                .produtos(List.of(
                        Carrinho.ItemCarrinho.builder()
                                .idProduto(idProduto)
                                .quantidade(quantidade)
                                .build()
                ))
                .build();
    }

    public static Carrinho comProdutos(List<Carrinho.ItemCarrinho> itens) {
        return Carrinho.builder()
                .produtos(itens)
                .build();
    }
}
