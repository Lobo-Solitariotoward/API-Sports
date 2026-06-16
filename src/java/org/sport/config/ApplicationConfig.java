package org.sport.config;

import jakarta.ws.rs.core.Application;
import org.sport.rest.AuthResource;
import org.sport.rest.CarritoResource;
import org.sport.rest.CategoriaResource;
import org.sport.rest.CuponResource;
import org.sport.rest.PedidoResource;
import org.sport.rest.ProductoResource;
import org.sport.rest.ProveedorResource;
import org.sport.rest.RestockResource;
import org.sport.rest.UsuarioResource;

import java.util.HashSet;
import java.util.Set;

public class ApplicationConfig extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(AuthResource.class);
        resources.add(UsuarioResource.class);
        resources.add(CategoriaResource.class);
        resources.add(ProductoResource.class);
        resources.add(CarritoResource.class);
        resources.add(CuponResource.class);
        resources.add(PedidoResource.class);
        resources.add(ProveedorResource.class);
        resources.add(RestockResource.class);
        return resources;
    }
}
