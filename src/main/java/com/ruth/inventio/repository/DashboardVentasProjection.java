package com.ruth.inventio.repository;

import java.math.BigDecimal;

public interface DashboardVentasProjection {
    BigDecimal getTotal();
    BigDecimal getSaldo();
    Long getVentasHoy();
}
