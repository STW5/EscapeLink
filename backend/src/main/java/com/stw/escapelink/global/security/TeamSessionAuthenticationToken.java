package com.stw.escapelink.global.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class TeamSessionAuthenticationToken extends AbstractAuthenticationToken {

    private final CurrentTeam principal;

    public TeamSessionAuthenticationToken(CurrentTeam principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_TEAM")));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
