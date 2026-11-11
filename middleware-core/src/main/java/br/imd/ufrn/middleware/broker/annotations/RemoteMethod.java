package br.imd.ufrn.middleware.broker.annotations;

import br.imd.ufrn.middleware.broker.enums.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Modelo de componentes: expõe o método como operação remota. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteMethod {

    String path();

    HttpMethod httpMethod() default HttpMethod.GET;
}
