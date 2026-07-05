package com.trisha.Loc.loc.model.enums;

/**
 * Quem pode acompanhar ao vivo a localizacao de uma sessao de rastreamento.
 * Escolhido pelo usuario ao iniciar a sessao (e alteravel durante ela);
 * o default seguro e PRIVADO.
 */
public enum SessionVisibility {
    /** Qualquer pessoa (ate estranhos) pode acompanhar ao vivo. */
    PUBLICO,
    /** Quem segue o usuario pode acompanhar (seguir nao exige aceite). */
    SEGUIDORES,
    /** Apenas amigos do usuario podem acompanhar. */
    AMIGOS,
    /** Ninguem acompanha ao vivo — so o proprio usuario. */
    PRIVADO
}
