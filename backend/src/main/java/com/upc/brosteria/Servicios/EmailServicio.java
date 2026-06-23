package com.upc.brosteria.Servicios;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServicio {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Async
    public void enviarCorreoHTML(String destinatario, String asunto, String contenidoHtml) {
        if (mailSender == null) {
            System.out.println("[Email Mock] Enviando correo a " + destinatario + " con asunto: " + asunto);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenidoHtml, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar email por SMTP a " + destinatario + ": " + e.getMessage());
        }
    }

    public void notificarStockBajo(String insumoNombre, Double actualStock, String unidad) {
        String asunto = "⚠️ ALERTA DE STOCK CRÍTICO: " + insumoNombre.toUpperCase();
        String html = """
            <div style="font-family: Arial, sans-serif; border: 1px solid #FF1744; border-radius: 8px; padding: 20px; max-width: 600px;">
                <h2 style="color: #FF1744; margin-top: 0;">¡Alerta de Inventario en La Brostería!</h2>
                <p>El insumo <strong>%s</strong> ha alcanzado su límite mínimo de seguridad.</p>
                <div style="background-color: #ffebee; border-left: 5px solid #FF1744; padding: 10px; margin: 15px 0;">
                    <strong>Stock Actual:</strong> %s %s
                </div>
                <p>Por favor, realice el pedido de reposición lo antes posible.</p>
                <hr style="border: 0; border-top: 1px solid #eee; margin-top: 20px;">
                <p style="font-size: 12px; color: #888;">CRM La Brostería - Sistema Automático de Notificaciones</p>
            </div>
            """.formatted(insumoNombre, actualStock, unidad);
        
        enviarCorreoHTML("labrosteriapremium@gmail.com", asunto, html);
    }
}
