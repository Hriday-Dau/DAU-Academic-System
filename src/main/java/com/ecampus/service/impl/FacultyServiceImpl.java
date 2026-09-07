package com.ecampus.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecampus.model.AcademicYears;
import com.ecampus.model.Courses;
import com.ecampus.model.Egcrstt1;
import com.ecampus.model.Terms;
import com.ecampus.model.Users;
import com.ecampus.repository.AcademicYearsRepository;
import com.ecampus.repository.CoursesRepository;
import com.ecampus.repository.Egcrstt1Repository;
import com.ecampus.repository.TermsRepository;
import com.ecampus.repository.UserRepository;
import com.ecampus.service.FacultyService;

@Service
public class FacultyServiceImpl implements FacultyService {

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private Egcrstt1Repository examTypeRepository;

    @Autowired
    private AcademicYearsRepository academicYearsRepository;

    @Autowired
    private CoursesRepository coursesRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Terms> getAllTerms() {
        return termsRepository.findAll();
    }

    @Override
    public List<Egcrstt1> getAllExamTypes() {
        return examTypeRepository.findAll();
    }

    @Override
    public List<AcademicYears> getAllAcademicYears() {
        return academicYearsRepository.findAll();
    }

    @Override
    public List<Courses> getAllCourses() {
        return coursesRepository.findAll();
    }

    @Override
public List<Users> getAllFaculties() {
    return userRepository.findAllFacultyList(); // see repo method below
}

@Override
public List<Users> searchFaculties(String keyword) {
    return userRepository.searchFacultyList(keyword);
}

}