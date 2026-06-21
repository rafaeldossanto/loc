package com.trisha.Loc.loc.model.enums;

/**
 * Quem pode acompanhar ao vivo a localizacao de uma sessao de rastreamento.
 * Escolhido pelo usuario ao iniciar a sessao; o default seguro e PRIVADO.
 */
public enum SessionVisibility {
    /** Qualquer pessoa (ate estranhos) pode acompanhar ao vivo. */
    PUBLICO,
    /** Apenas amigos do usuario podem acompanhar. */
    AMIGOS,
    /** Ninguem acompanha ao vivo — so o proprio usuario. */
    PRIVADO
}
