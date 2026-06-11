package com.ecampus.controller;

import com.ecampus.service.GlobalConstantsService;
import com.ecampus.session.SessionVars;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalUserAdvice {

    private final SessionVars sessionVars;
    private final GlobalConstantsService globalConstantsService;

    public GlobalUserAdvice(SessionVars sessionVars,
                            GlobalConstantsService globalConstantsService) {
        this.sessionVars = sessionVars;
        this.globalConstantsService = globalConstantsService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model) {

        model.addAttribute("sessionVars", sessionVars);

        model.addAttribute(
                "currentAcademicYearName",
                getDisplayValue(globalConstantsService.getCurrentAcademicYearName())
        );

        model.addAttribute(
                "currentTermName",
                getDisplayValue(globalConstantsService.getCurrentTermName())
        );
    }

    private String getDisplayValue(String value) {
        if (value == null || value.isBlank()) {
            return "Not Set";
        }
        return value;
    }
}





// package com.ecampus.controller;

// import com.ecampus.model.Users;
// import com.ecampus.repository.UserRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.ControllerAdvice;
// import org.springframework.web.bind.annotation.ModelAttribute;

// import java.util.Optional;

// @ControllerAdvice
// public class GlobalUserAdvice {

//     @Autowired
//     private UserRepository userRepository;

//     /**
//      * This method runs before every controller request. 
//      * It puts the full 'Users' object into the model as 'currentUser'.
//      */
//     @ModelAttribute
//     public void addUserToModel(Model model) {
//         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
//         if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
//             String username = auth.getName();
//             Optional<Users> userOpt = userRepository.findWithName(username);
            
//             userOpt.ifPresent(user -> model.addAttribute("currentUser", user));
//         }
//     }
// }