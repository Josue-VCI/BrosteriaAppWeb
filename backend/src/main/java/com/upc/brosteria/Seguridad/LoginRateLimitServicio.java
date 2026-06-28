package com.upc.brosteria.Seguridad;

import com.upc.brosteria.Excepciones.DemasiadosIntentosException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimitServicio {

    private static final int MAX_INTENTOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, EstadoIntentos> intentos = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginRateLimitServicio() {
        this(Clock.systemUTC());
    }

    LoginRateLimitServicio(Clock clock) {
        this.clock = clock;
    }

    public void verificarPermitido(String email) {
        String clave = normalizar(email);
        EstadoIntentos estado = intentos.get(clave);
        Instant ahora = clock.instant();
        if (estado != null && estado.bloqueadoHasta != null && estado.bloqueadoHasta.isAfter(ahora)) {
            throw new DemasiadosIntentosException();
        }
        if (estado != null && estado.inicioVentana.plus(VENTANA).isBefore(ahora)) {
            intentos.remove(clave, estado);
        }
    }

    public void registrarFallo(String email) {
        String clave = normalizar(email);
        Instant ahora = clock.instant();
        intentos.compute(clave, (key, actual) -> {
            EstadoIntentos estado = actual;
            if (estado == null || estado.inicioVentana.plus(VENTANA).isBefore(ahora)) {
                estado = new EstadoIntentos(0, ahora, null);
            }
            int fallos = estado.fallos + 1;
            Instant bloqueadoHasta = fallos >= MAX_INTENTOS ? ahora.plus(VENTANA) : null;
            return new EstadoIntentos(fallos, estado.inicioVentana, bloqueadoHasta);
        });
    }

    public void registrarExito(String email) {
        intentos.remove(normalizar(email));
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record EstadoIntentos(int fallos, Instant inicioVentana, Instant bloqueadoHasta) {
    }
}
