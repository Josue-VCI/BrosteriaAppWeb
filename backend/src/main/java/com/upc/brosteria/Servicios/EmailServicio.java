package com.upc.brosteria.Servicios;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailServicio {

    private static final Logger log = LoggerFactory.getLogger(EmailServicio.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:josuebecerrav19@gmail.com}")
    private String fromEmail;

    @Value("${stock.alert.email}")
    private String stockAlertEmail;

    @Value("${app.frontend.url:https://brosteria-app-web.vercel.app}")
    private String frontendUrl;

    @Async
    public void enviarCorreoHTML(String destinatario, String asunto, String contenidoHtml) {
        if (mailSender == null) {
            log.info("Email mock para {} con asunto {}", destinatario, asunto);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "La Brosteria");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenidoHtml, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error al enviar email por SMTP a {}", destinatario, e);
        }
    }

    @Async
    public void notificarStockBajo(String insumoNombre, BigDecimal actualStock, String unidad) {
        String asunto = "Stock bajo: " + insumoNombre;
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:480px;padding:24px;border:1px solid #eee;border-radius:8px">
                <h2 style="color:#FF1744;margin:0 0 8px">Stock bajo</h2>
                <p><strong>%s</strong> tiene %s %s disponibles.</p>
                <a href="%s/inventario" style="display:inline-block;background:#FF6B00;color:#fff;text-decoration:none;padding:12px 18px;border-radius:6px;font-weight:bold">Revisar inventario</a>
            </div>
            """.formatted(HtmlUtils.htmlEscape(insumoNombre), actualStock, HtmlUtils.htmlEscape(unidad),
                    HtmlUtils.htmlEscape(frontendUrl));
        
        enviarCorreoHTML(stockAlertEmail, asunto, html);
    }
}
