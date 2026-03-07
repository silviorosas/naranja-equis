package com.naranjax.notification.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    // El servicio es 100% orientado a eventos; no requiere RestTemplate ni
    // LoadBalancer.
}
