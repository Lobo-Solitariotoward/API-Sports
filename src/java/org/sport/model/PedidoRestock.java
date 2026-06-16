package org.sport.model;

public class PedidoRestock {
    public Integer id_restock;
    public Integer id_producto;
    public Integer id_proveedor;
    public Integer cantidad_solicitada;
    public String fecha_solicitud;
    public String fecha_estimada_entrega;
    public String fecha_real_entrega;
    public String estado;
    public String notas;
}
