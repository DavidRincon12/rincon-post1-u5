package com.universidad.reservaslabs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class RelojConfig {

    // Se inyecta el reloj para poder fijar la hora actual en las pruebas
    // de la regla de cancelación.
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
