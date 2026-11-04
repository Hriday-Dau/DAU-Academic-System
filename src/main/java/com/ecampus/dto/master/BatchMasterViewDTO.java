package com.ecampus.dto.master;

public record BatchMasterViewDTO(
        Long bchid,
        String bchname,
        String prgname,
        String splname,
        Long bchcapacity,
        String bchinstcode,
        Long studentCount
) {}

