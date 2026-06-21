package com.trisha.Loc.loc.trace;

import org.slf4j.MDC;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Correlacao de requisicoes (traceId). Inteiro de 6 digitos gerado na borda
 * (BFF) e propagado via header {@code X-Trace-Id}; este servico o le, coloca no
 * MDC (logs) e grava nas linhas que cria.
 */
public final class TraceContext {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private TraceContext() {
    }

    /** traceId atual da requisicao (do MDC), ou null fora de uma requisicao. */
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    /** Gera um traceId: inteiro aleatorio de 6 digitos (100000-999999). */
    public static String generate() {
        return Integer.toString(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
    }
}
