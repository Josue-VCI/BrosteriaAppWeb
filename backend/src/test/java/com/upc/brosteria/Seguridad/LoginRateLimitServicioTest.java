package com.upc.brosteria.Seguridad;

import com.upc.brosteria.Excepciones.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimitServicioTest {

    @Test
    void bloqueaDespuesDeCincoFallosYReiniciaConExito() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-28T12:00:00Z"), ZoneOffset.UTC);
        LoginRateLimitServicio servicio = new LoginRateLimitServicio(clock);

        for (int i = 0; i < 5; i++) {
            servicio.verificarPermitido("admin@brosteria.com");
            servicio.registrarFallo("admin@brosteria.com");
        }

        assertThrows(DemasiadosIntentosException.class,
                () -> servicio.verificarPermitido("admin@brosteria.com"));

        servicio.registrarExito("admin@brosteria.com");
        assertDoesNotThrow(() -> servicio.verificarPermitido("admin@brosteria.com"));
    }
}
