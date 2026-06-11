package com.ecampus.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.ecampus.model.StudentGradeDTO;

public interface FileService {

    boolean hasCsvFormat(MultipartFile file);

    List<StudentGradeDTO> csvToStudentGradeDTOs(InputStream inputStream);

    boolean hasPdfFormat(MultipartFile file);

    Map<String, Object> uploadCoursePdf(MultipartFile file, Long tcrid, String courseCode) throws Exception;

    String getUploadDirectory();
}
