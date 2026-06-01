package com.ecampus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "semestercourses", schema="ec2")
public class SemesterCourses {

    @Id
    @Column(name = "scrid")
    private Long scrid;

    @Column(name = "scrstrid")
    private Long scrstrid;

    @Column(name = "screlective")
    private String screlective;

    @Column(name = "scrcrsid")
    private Long scrcrsid;

    @Column(name = "scrcgpid")
    private Long scrcgpid;

    @Column(name = "scrcreatedby")
    private Long scrcreatedby = 0L;

    @Column(name = "scrcreatedat")
    private LocalDateTime scrcreatedat = LocalDateTime.now();

    @Column(name = "scrlastupdatedby")
    private Long scrlastupdatedby;

    @Column(name = "scrlastupdatedat")
    private LocalDateTime scrlastupdatedat;

    @Column(name = "scrrowstate")
    private Long scrrowstate = 1L;

    @Column(name = "scrtcrid")
    private Long scrtcrid;

    @Column(name = "scrseqno")
    private Long scrseqno;

    @Column(name = "ctpid")
    private Long ctpid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ctpid", insertable = false, updatable = false)
    private CourseTypes courseType;

    // Auto-default setter before inserting
    @PrePersist
    public void prePersist() {
        if (this.scrcreatedby == null) this.scrcreatedby = 0L;
        if (this.scrcreatedat == null) this.scrcreatedat = LocalDateTime.now();
        if (this.scrrowstate == null) this.scrrowstate = 1L;
    }

    // Getters & Setters
    public Long getScrid() { return scrid; }
    public void setScrid(Long scrid) { this.scrid = scrid; }

    public Long getScrstrid() { return scrstrid; }
    public void setScrstrid(Long scrstrid) { this.scrstrid = scrstrid; }

    public String getScrelective() { return screlective; }
    public void setScrelective(String screlective) { this.screlective = screlective; }

    public Long getScrcrsid() { return scrcrsid; }
    public void setScrcrsid(Long scrcrsid) { this.scrcrsid = scrcrsid; }

    public Long getScrcgpid() { return scrcgpid; }
    public void setScrcgpid(Long scrcgpid) { this.scrcgpid = scrcgpid; }

    public Long getScrcreatedby() { return scrcreatedby; }
    public void setScrcreatedby(Long scrcreatedby) { this.scrcreatedby = scrcreatedby; }

    public LocalDateTime getScrcreatedat() { return scrcreatedat; }
    public void setScrcreatedat(LocalDateTime scrcreatedat) { this.scrcreatedat = scrcreatedat; }

    public Long getScrlastupdatedby() { return scrlastupdatedby; }
    public void setScrlastupdatedby(Long scrlastupdatedby) { this.scrlastupdatedby = scrlastupdatedby; }

    public LocalDateTime getScrlastupdatedat() { return scrlastupdatedat; }
    public void setScrlastupdatedat(LocalDateTime scrlastupdatedat) { this.scrlastupdatedat = scrlastupdatedat; }

    public Long getScrrowstate() { return scrrowstate; }
    public void setScrrowstate(Long scrrowstate) { this.scrrowstate = scrrowstate; }

    public Long getScrtcrid() { return scrtcrid; }
    public void setScrtcrid(Long scrtcrid) { this.scrtcrid = scrtcrid; }

    public Long getScrseqno() { return scrseqno; }
    public void setScrseqno(Long scrseqno) { this.scrseqno = scrseqno; }

    public Long getCtpid() { return ctpid; }
    public void setCtpid(Long ctpid) { this.ctpid = ctpid; }
    
    public CourseTypes getCourseType() { return courseType; }
    public void setCourseType(CourseTypes courseType) { this.courseType = courseType; }
}
