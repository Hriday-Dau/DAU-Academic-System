package com.ecampus.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Helper class for composite key (gmdid + gmdstdid)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeModRequestDetailsId implements Serializable {

    private Long gmdId;
    private Long gmdStdId;
}