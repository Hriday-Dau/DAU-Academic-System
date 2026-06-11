package com.ecampus.auth.service;

import com.ecampus.model.Users;
import com.ecampus.repository.UserRepository;
import com.ecampus.session.SessionVars;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Map;

public class RoleAwareSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final Map<String, String> defaultSuccessUrls;
    private final UserRepository userRepository;
    private final SessionVars sessionVars;

    public RoleAwareSuccessHandler(
            Map<String, String> defaultSuccessUrls,
            UserRepository userRepository,
            SessionVars sessionVars) {

        this.defaultSuccessUrls = defaultSuccessUrls;
        this.userRepository = userRepository;
        this.sessionVars = sessionVars;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        loadSessionUser(authentication);

        super.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected String determineTargetUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();

            if (defaultSuccessUrls.containsKey(role)) {
                return defaultSuccessUrls.get(role);
            }
        }

        return "/login?error";
    }

    private void loadSessionUser(Authentication authentication) {

        if (authentication == null ||
                authentication.getName() == null ||
                authentication.getName().isBlank()) {

            sessionVars.clear();
            return;
        }

        String loginValue = authentication.getName();

        Users user = userRepository.findWithName(loginValue).orElse(null);

        if (user == null ||
                user.getUnivId() == null ||
                user.getUnivId().isBlank()) {

            sessionVars.clear();
            return;
        }

        sessionVars.loadFrom(user);
    }
}