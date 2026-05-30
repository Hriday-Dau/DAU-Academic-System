package com.ecampus.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_trail", schema = "ec2")
@IdClass(WorkTrailId.class) // composite key helper class
@Data
public class WorkTrail {

    @Id
    @Column(name = "work_id")
    private Long workId;

    @Id
    @Column(name = "node_number", nullable = false)
    private Integer nodeNumber;

    @Id
    @Column(name = "iteration_number", nullable = false)
    private Integer iterationNumber = 1;

    @Column(name = "work_type_code", nullable = false)
    private Long workTypeCode;

    @Column(name = "response_code", nullable = false)
    private Integer responseCode;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "response_date")
    private LocalDateTime responseDate;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "prev_node_number")
    private Integer prevNodeNumber;

    @Column(name = "prev_employee_id")
    private Long prevEmployeeId;

    @Column(name = "prev_iteration_number")
    private Integer prevIterationNumber;
}