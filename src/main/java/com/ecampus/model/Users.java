package com.ecampus.model;

import java.time.LocalDateTime;

import com.ecampus.auth.user.AuthUserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", schema = "ec2")
public class Users implements AuthUserDetails {

    private static final String ROLE_ADMIN = "901";
    private static final String ROLE_DEAN = "902";
    private static final String ROLE_REGISTRAR = "905";
    private static final String ROLE_FACULTY = "913";
    private static final String ROLE_STUDENT = "914";

    @Id
    @Column(name = "uid")
    private Long uid;

    @Column(name = "univid")
    private String univId;

    @Column(name = "stdid")
    private Long stdid;

    @Column(name = "uname")
    private String uname;

    @Column(name = "ufullname")
    private String ufullname;

    @Column(name = "urole")
    private String urole;

    @Column(name = "uemail")
    private String uemail;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "create_ts")
    private LocalDateTime createTs;

    @Column(name = "last_updated_by")
    private Long lastUpdatedBy;

    @Column(name = "last_updated_ts")
    private LocalDateTime lastUpdatedTs;

    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "row_state")
    private Short rowState;

    @Column(name = "uid_older")
    private Long uidOlder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "urole", referencedColumnName = "rid", insertable = false, updatable = false)
    private Role role;

    public Users() {
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getUnivId() {
        return univId;
    }

    public void setUnivId(String univId) {
        this.univId = univId;
    }

    public Long getStdid() {
        return stdid;
    }

    public void setStdid(Long stdid) {
        this.stdid = stdid;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getUfullname() {
        return ufullname;
    }

    public void setUfullname(String ufullname) {
        this.ufullname = ufullname;
    }

    public String getUrole() {
        return urole;
    }

    public void setUrole(String urole) {
        this.urole = urole;
    }

    public String getUemail() {
        return uemail;
    }

    public void setUemail(String uemail) {
        this.uemail = uemail;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTs() {
        return createTs;
    }

    public void setCreateTs(LocalDateTime createTs) {
        this.createTs = createTs;
    }

    public Long getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(Long lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public LocalDateTime getLastUpdatedTs() {
        return lastUpdatedTs;
    }

    public void setLastUpdatedTs(LocalDateTime lastUpdatedTs) {
        this.lastUpdatedTs = lastUpdatedTs;
    }

    @Override
    public String getpassword() {
        return this.password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;


    }

    public Short getRowState() {
        return rowState;
    }

    public void setRowState(Short rowState) {
        this.rowState = rowState;
    }

    public Long getUidOlder() {
        return uidOlder;
    }

    public void setUidOlder(Long uidOlder) {
        this.uidOlder = uidOlder;
    }

    public Role getRole() {
        return role;
    }

    public Role getRoleEntity() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setRoleEntity(Role role) {
        this.role = role;
    }

    @Override
    public String getUsername() {
        return this.uname;
    }

    @Override
    public String getrole() {
        if (ROLE_STUDENT.equals(this.urole)) {
            return "STUDENT";
        }

        if (ROLE_FACULTY.equals(this.urole)) {
            return "FACULTY";
        }

        if (ROLE_ADMIN.equals(this.urole)) {
            return "ADMIN";
        }

        if (ROLE_REGISTRAR.equals(this.urole)) {
            return "REGISTRAR";
        }

        if (ROLE_DEAN.equals(this.urole)) {
            return "DEAN";
        }

        return "UNKNOWN";
    }
}