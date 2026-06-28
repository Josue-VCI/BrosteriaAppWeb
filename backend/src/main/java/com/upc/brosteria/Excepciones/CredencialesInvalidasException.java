package com.upc.brosteria.Excepciones;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contrasena incorrectos");
    }
}
