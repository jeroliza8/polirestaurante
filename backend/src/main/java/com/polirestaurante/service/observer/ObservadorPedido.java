package com.polirestaurante.service.observer;

/** Observer: Cocina no conoce controladores ni vistas de otros actores. */
public interface ObservadorPedido {
    void actualizar(EventoCocina evento);
}
