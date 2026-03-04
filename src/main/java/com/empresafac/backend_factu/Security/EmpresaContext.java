package com.empresafac.backend_factu.Security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class EmpresaContext {

    public Long getEmpresaIdActual() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        return (Long) auth.getDetails();
    }

    public String getUsernameActual() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        return auth.getName();
    }
}
