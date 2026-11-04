package com.ecampus.dto.master;

public class RoleCreateForm {

    private String rid;
    private String roleName;
    private String comments;

    public RoleCreateForm() {}

    public RoleCreateForm(String rid, String roleName, String comments) {
        this.rid = rid;
        this.roleName = roleName;
        this.comments = comments;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}

