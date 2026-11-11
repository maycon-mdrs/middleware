package br.imd.ufrn.middleware.broker.annotations;

import br.imd.ufrn.middleware.broker.enums.LifecyclePolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Modelo de componentes: marca a classe de negócio como objeto remoto. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteComponent {

    String name();

    LifecyclePolicy lifecycle() default LifecyclePolicy.STATIC;
}
