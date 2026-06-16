package org.sport.model;

import java.math.BigDecimal;

public class Pedido {
    public Integer id_pedido;
    public Integer id_usuario;
    public String fecha_pedido;
    public BigDecimal total;
    public String estado;
    public String direccion_envio;
    public String metodo_pago;
    public Integer id_cupon_aplicado;
    public String notas;
}
