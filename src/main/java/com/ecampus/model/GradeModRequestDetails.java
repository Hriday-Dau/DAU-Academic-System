package com.ecampus.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "grademodrequestdetails", schema = "ec2")
@IdClass(GradeModRequestDetailsId.class)
@Data
public class GradeModRequestDetails {
    
    // Compostie PK: (gmdid + gmdstdid)
    @Id
    @Column(name = "gmdid")
    private Long gmdId;

    @Id
    @Column(name = "gmdstdid")
    private Long gmdStdId;

    @Column(name = "gmdpresentgrade", nullable = false)
    private Long gmdPresentGrade;

    @Column(name = "gmdnewgrade", nullable = false)
    private Long gmdNewGrade;

    @Column(name = "gmdchangedesc")
    private String gmdChangeDesc;

    @Column(name = "gmdrowstate", nullable = false)
    private Short gmdRowState = 1;
}