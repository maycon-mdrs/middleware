package br.imd.ufrn.middleware.broker.enums;

/** Lifecycle Management: como o middleware obtém a instância do objeto remoto. */
public enum LifecyclePolicy {
    STATIC,
    PER_REQUEST,
    CLIENT_DEPENDENT,
    LAZY_ACQUISITION,
    POOLING,
    LEASING,
    PASSIVATION
}
