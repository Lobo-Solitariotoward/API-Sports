package org.sport.model;

import java.math.BigDecimal;

public class Producto {
    public Integer id_producto;
    public String nombre;
    public String descripcion;
    public Integer id_categoria;
    public String marca;
    public BigDecimal precio_normal;
    public BigDecimal precio_oferta;
    public Integer stock_actual;
    public Integer stock_minimo;
    public String imagen_url;
    public Boolean activo;
    public String fecha_creacion;
}
