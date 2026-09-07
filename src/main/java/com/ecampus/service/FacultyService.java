package com.ecampus.service;

import com.ecampus.model.*;
import org.springframework.data.domain.Page;
import java.util.List;

public interface FacultyService {

    List<Terms> getAllTerms();
    List<Egcrstt1> getAllExamTypes();
    List<AcademicYears> getAllAcademicYears();
    List<Courses> getAllCourses();



    // Non-paginated versions
    List<Users> getAllFaculties();
    List<Users> searchFaculties(String keyword);
}
