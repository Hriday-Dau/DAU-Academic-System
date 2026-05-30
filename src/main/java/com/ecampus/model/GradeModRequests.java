package com.ecampus.model;

import jakarta.persistence.*;
import lombok.Data; //this library gives us the Data annotation for getters and setters
import java.time.LocalDateTime;

@Entity
@Table(name = "grademodrequests", schema = "ec2")
@Data //automatic getters and setters
public class GradeModRequests {

    @Id
    @Column(name = "gmdreq_id")
    private Long gmdReqId; // Links to work_id, but not as FK

    @Column(name = "gmdtcrid", nullable = false)
    private Integer gmdTcrId;

    @Column(name = "gmdreqdesc")
    private String gmdReqDesc;

    @Column(name = "gmdcreatedby", nullable = false)
    private Long gmdCreatedBy;

    @Column(name = "gmdcreatedt")
    private LocalDateTime gmdCreatedAt;

    @Column(name = "gmd_approvalstatus", nullable = false)
    private String gmdApprovalStatus = "Pending Dean Approval";

    @Column(name = "gmdexamtype_id", nullable = false)
    private Long gmdExamTypeId = 1L;

    @Column(name = "gmdupdatedby")
    private Long gmdUpdatedBy;

    @Column(name = "gmdupdatedt")
    private LocalDateTime gmdUpdatedAt;

    @Column(name = "gmdreq_rowstate", nullable = false)
    private Short gmdReqRowState = 1;

    @Column(name = "gmditerationno", nullable = false)
    private Short gmdIterationNo = 1;
}