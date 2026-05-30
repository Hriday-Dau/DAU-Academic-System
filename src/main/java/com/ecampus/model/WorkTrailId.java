package com.ecampus.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkTrailId implements Serializable {
    private Long workId;
    private Integer nodeNumber;
    private Integer iterationNumber;
}