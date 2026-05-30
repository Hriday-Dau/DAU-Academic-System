package com.ecampus.controller;

import com.ecampus.model.Users;
import com.ecampus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalUserAdvice {

    @Autowired
    private UserRepository userRepository;

    /**
     * This method runs before every controller request. 
     * It puts the full 'Users' object into the model as 'currentUser'.
     */
    @ModelAttribute
    public void addUserToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            String username = auth.getName();
            Optional<Users> userOpt = userRepository.findWithName(username);
            
            userOpt.ifPresent(user -> model.addAttribute("currentUser", user));
        }
    }
}