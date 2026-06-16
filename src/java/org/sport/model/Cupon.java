package org.sport.model;

import java.math.BigDecimal;

public class Cupon {
    public Integer id_cupon;
    public String codigo;
    public String tipo;
    public BigDecimal valor;
    public String fecha_inicio;
    public String fecha_fin;
    public Integer max_uso;
    public Integer usos_actuales;
    public Boolean activo;
}
