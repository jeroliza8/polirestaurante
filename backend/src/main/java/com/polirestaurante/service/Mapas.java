package com.polirestaurante.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Indexa una lista por clave tolerando valores nulos (Collectors.toMap no los admite). */
public final class Mapas {

    private Mapas() {
    }

    public static <T, K, V> Map<K, V> indexar(Collection<T> elementos, Function<T, K> clave, Function<T, V> valor) {
        Map<K, V> mapa = new HashMap<>();
        for (T e : elementos) {
            mapa.putIfAbsent(clave.apply(e), valor.apply(e));
        }
        return mapa;
    }
}
