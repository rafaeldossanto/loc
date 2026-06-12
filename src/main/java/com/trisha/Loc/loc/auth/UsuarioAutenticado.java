package com.trisha.Loc.loc.auth;

/**
 * Identidade do usuario autenticado, extraida dos claims do JWT validado.
 */
public record UsuarioAutenticado(String id, String codigoUsuario, String email) {
}
