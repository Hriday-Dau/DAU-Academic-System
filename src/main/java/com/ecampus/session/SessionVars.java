package com.ecampus.session;

import com.ecampus.model.Users;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

@Component
@SessionScope
public class SessionVars implements Serializable {

    private Users theUser;

    public void loadFrom(Users user) {
        this.theUser = user;
    }

    public void clear() {
        this.theUser = null;
    }

    public Users getLoggedInUser() {
        return theUser;
    }

    public Long getUid() {
        return theUser == null ? null : theUser.getUid();
    }

    public String getUnivid() {
        return theUser == null ? null : theUser.getUnivId();
    }

    public Long getStdid() {
        return theUser == null ? null : theUser.getStdid();
    }

    public String getUname() {
        return theUser == null ? null : theUser.getUname();
    }

    public String getUfullname() {
        return theUser == null ? null : theUser.getUfullname();
    }

    public String getUrole() {
        return theUser == null ? null : theUser.getUrole();
    }

    public String getUemail() {
        return theUser == null ? null : theUser.getUemail();
    }

    public boolean isAuthenticated() {
        return theUser != null;
    }
}
