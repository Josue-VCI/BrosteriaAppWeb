package com.upc.brosteria.Excepciones;

public class DemasiadosIntentosException extends RuntimeException {
    public DemasiadosIntentosException() {
        super("Demasiados intentos fallidos. Intente nuevamente en 15 minutos");
    }
}
