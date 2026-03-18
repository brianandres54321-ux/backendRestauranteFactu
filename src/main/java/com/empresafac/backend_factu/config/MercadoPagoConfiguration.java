package com.empresafac.backend_factu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.mercadopago.MercadoPagoConfig;

import jakarta.annotation.PostConstruct;

/**
 * Inicializa el SDK de Mercado Pago con tu Access Token.
 *
 * En application.properties agrega:
 * mercadopago.access-token=TU_ACCESS_TOKEN_AQUI
 *
 * Para pruebas usa el Access Token de TEST que te da el panel de MercadoPago.
 */
@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }
}