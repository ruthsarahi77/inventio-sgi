package com.ruth.inventio.config;

import java.time.ZoneId;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Datos institucionales del documento, configurables con INVENTIO_PDF_*. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "inventio.pdf")
public class ProformaPdfProperties {
    private String empresa = "BUTRÓN COMERCIO INTERNACIONAL";
    private String distribuidor = "DISTRIBUIDOR AUTORIZADO REPSOL";
    private String ruc = "2061128732001";
    private String direccion = "MZC LOTE 19 TACNA";
    private String telefono = "";
    private String correo = "";
    private String logo = "classpath:pdf/repsol.png";
    private ZoneId zonaHoraria = ZoneId.of("America/Lima");
    // Convención para el modelo actual, que no persiste moneda por documento.
    private Moneda moneda = Moneda.PEN;
    public enum Moneda { PEN, USD }
}
