package com.ecampus.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecampus.dto.StudentResultCourseProjection;
import com.ecampus.dto.StudentResultViewDTO;
import com.ecampus.dto.StudentResultViewDTO.CourseRow;
import com.ecampus.dto.StudentResultViewDTO.Performance;
import com.ecampus.dto.StudentResultViewDTO.SemesterOption;
import com.ecampus.model.Batches;
import com.ecampus.model.CourseTypes;
import com.ecampus.model.Courses;
import com.ecampus.model.Egcrstt1;
import com.ecampus.model.Eggradm1;
import com.ecampus.model.Semesters;
import com.ecampus.model.StudentRegistrationCourses;
import com.ecampus.model.StudentRegistrations;
import com.ecampus.model.StudentSemesterResult;
import com.ecampus.model.Students;
import com.ecampus.model.TermCourses;
import com.ecampus.model.Terms;
import com.ecampus.repository.Egcrstt1Repository;
import com.ecampus.repository.StudentRegistrationCoursesRepository;
import com.ecampus.repository.StudentRegistrationsRepository;
import com.ecampus.repository.StudentSemesterResultRepository;
import com.ecampus.repository.TermCoursesRepository;

@Service
public class StudentResultService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private final StudentRegistrationsRepository studentRegistrationsRepository;
    private final StudentRegistrationCoursesRepository studentRegistrationCoursesRepository;
    private final StudentSemesterResultRepository studentSemesterResultRepository;
    private final TermCoursesRepository termCoursesRepository;
    private final Egcrstt1Repository egcrstt1Repository;

    public StudentResultService(StudentRegistrationsRepository studentRegistrationsRepository,
                                StudentRegistrationCoursesRepository studentRegistrationCoursesRepository,
                                StudentSemesterResultRepository studentSemesterResultRepository,
                                TermCoursesRepository termCoursesRepository,
                                Egcrstt1Repository egcrstt1Repository) {
        this.studentRegistrationsRepository = studentRegistrationsRepository;
        this.studentRegistrationCoursesRepository = studentRegistrationCoursesRepository;
        this.studentSemesterResultRepository = studentSemesterResultRepository;
        this.termCoursesRepository = termCoursesRepository;
        this.egcrstt1Repository = egcrstt1Repository;
    }

    @Transactional(readOnly = true)
    public StudentResultViewDTO buildResult(Long studentId, Long semesterId) {
        StudentResultViewDTO view = new StudentResultViewDTO();

        if (studentId == null) {
            view.setMessage("Student record is not linked with this session.");
            return view;
        }

        List<StudentRegistrations> registrations = studentRegistrationsRepository
                .findAllRegistrationsByStudentIdOrderBySemesterSequence(studentId)
                .stream()
                .filter(reg -> reg.getSrgrowstate() == null || reg.getSrgrowstate() > 0)
                .toList();

        if (registrations.isEmpty()) {
            view.setMessage("No semester registration is available for this student.");
            return view;
        }

        view.setSemesters(registrations.stream()
                .map(reg -> new SemesterOption(reg.getSrgstrid(), formatSemester(reg.getSemester())))
                .toList());

        StudentRegistrations selectedRegistration = selectRegistration(registrations, semesterId);
        if (selectedRegistration == null) {
            view.setMessage("Selected semester is not available for this student.");
            return view;
        }

        Students student = selectedRegistration.getStudent();
        Batches batch = student != null ? student.getBatch() : null;
        view.setSelectedSemesterId(selectedRegistration.getSrgstrid());
        view.setStudentName(buildStudentName(student));
        view.setStudentInstId(student != null ? student.getStdinstid() : "");
        view.setBatchName(batch != null ? safeText(batch.getBchname()) : "");
        view.setProgramName(batch != null && batch.getPrograms() != null
                ? safeText(batch.getPrograms().getPrgname())
                : "");
        view.setSemesterName(formatSemester(selectedRegistration.getSemester()));
        view.setRegistrationDate(selectedRegistration.getSrgregdate() != null
                ? selectedRegistration.getSrgregdate().format(DATE_FORMAT)
                : "");
        StudentSemesterResult result = studentSemesterResultRepository
                .findByStudentRegistration_SrgidAndSsrrowstateGreaterThan(selectedRegistration.getSrgid(), (short) 0)
                .stream()
                .findFirst()
                .orElse(null);
        if (result == null) {
            view.setMessage("Result has not been published for the selected semester.");
            return view;
        }

        view.setAvailable(true);
        view.setCourses(fetchCourseRows(studentId, selectedRegistration.getSrgid()));
        view.setSemesterPerformance(new Performance(
                formatDecimal(result.getSsrcreditsregistered()),
                formatDecimal(result.getSsrcreditsearned()),
                formatDecimal(result.getSsrgradepointsearned()),
                preferNumeric(result.getSsrspi(), result.getSsrspiNumeric())));
        view.setCumulativePerformance(new Performance(
                formatDecimal(result.getSsrcumcreditsregistered()),
                formatDecimal(result.getSsrcumcreditsearned()),
                formatDecimal(result.getSsrcumgradepointsearned()),
                preferNumeric(result.getSsrcpi(), result.getSsrcpiNumeric())));
        return view;
    }

    private StudentRegistrations selectRegistration(List<StudentRegistrations> registrations, Long semesterId) {
        if (semesterId != null) {
            return registrations.stream()
                    .filter(reg -> Objects.equals(reg.getSrgstrid(), semesterId))
                    .findFirst()
                    .orElse(null);
        }
        return registrations.get(registrations.size() - 1);
    }

    private List<CourseRow> fetchCourseRows(Long studentId, Long registrationId) {
        List<StudentRegistrationCourses> registrations =
                studentRegistrationCoursesRepository.findBySrgId(registrationId);
        if (registrations.isEmpty()) {
            return List.of();
        }

        List<Long> termCourseIds = registrations.stream()
                .map(StudentRegistrationCourses::getSrctcrid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, TermCourses> termCoursesById = new HashMap<>();
        termCoursesRepository.findAllById(termCourseIds)
                .forEach(termCourse -> termCoursesById.put(termCourse.getTcrid(), termCourse));

        Map<Long, Egcrstt1> gradesByTermCourseId = termCourseIds.isEmpty()
                ? Map.of()
                : egcrstt1Repository.findByStudIdAndTcridIn(studentId, termCourseIds).stream()
                        .filter(this::hasActiveGradeRow)
                        .collect(Collectors.toMap(Egcrstt1::getTcrid, Function.identity(), (first, ignored) -> first));

        return registrations.stream()
                .map(registration -> toCourseProjection(
                        registration,
                        termCoursesById.get(registration.getSrctcrid()),
                        gradesByTermCourseId.get(registration.getSrctcrid())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StudentResultCourseProjection::getCourseCode,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::toCourseRow)
                .toList();
    }

    private StudentResultCourseProjection toCourseProjection(
            StudentRegistrationCourses registration,
            TermCourses termCourse,
            Egcrstt1 gradeRecord) {
        if (termCourse == null || termCourse.getCourse() == null) {
            return null;
        }

        Courses course = termCourse.getCourse();
        Eggradm1 grade = gradeRecord != null ? gradeRecord.getGrade() : null;
        return new StudentResultCourseProjection(
                safeText(course.getCrsname(), safeText(course.getCrstitle())),
                safeText(course.getCrscode()),
                resolveCourseType(registration, termCourse, course),
                course.getCrscreditpoints(),
                course.getCrslectures(),
                course.getCrstutorials(),
                course.getCrspracticals(),
                grade != null ? safeText(grade.getGradLt(), "--") : "--",
                grade != null && grade.getGradPt() != null
                        ? grade.getGradPt()
                        : gradeRecord != null ? gradeRecord.getObtCredits() : null,
                safeText(registration.getSrcfield1()));
    }

    private CourseRow toCourseRow(StudentResultCourseProjection row) {
        BigDecimal credits = safeDecimal(row.getCredits());
        BigDecimal gradePointValue = row.getGradePointValue();
        String gradePoints = gradePointValue == null
                ? "--"
                : formatDecimal(credits.multiply(gradePointValue));
        return new CourseRow(
                safeText(row.getCourseTitle()),
                safeText(row.getCourseCode()),
                safeText(row.getCourseType()),
                formatCreditHours(credits, safeDecimal(row.getLectures()),
                        safeDecimal(row.getTutorials()), safeDecimal(row.getPracticals())),
                safeText(row.getGradeLetter(), "--"),
                gradePoints,
                safeText(row.getRemarks()));
    }

    private String resolveCourseType(
            StudentRegistrationCourses registration,
            TermCourses termCourse,
            Courses course) {
        CourseTypes courseType = registration.getCurrentCourseType();
        if (courseType == null) {
            courseType = registration.getOriginalCourseType();
        }
        if (courseType != null && !safeText(courseType.getCtpcode()).isBlank()) {
            return safeText(courseType.getCtpcode());
        }
        if (!safeText(registration.getSrctype()).isBlank()) {
            return safeText(registration.getSrctype());
        }
        if (!safeText(termCourse.getCrstype()).isBlank()) {
            return safeText(termCourse.getCrstype());
        }
        return safeText(course.getCrstype());
    }

    private boolean hasActiveGradeRow(Egcrstt1 grade) {
        String rowState = grade.getRowSt();
        return rowState == null || !"0".equals(rowState.trim());
    }

    private String formatSemester(Semesters semester) {
        if (semester == null) {
            return "";
        }
        String name = safeText(semester.getStrname());
        Terms term = semester.getTerms();
        String termName = term != null ? safeText(term.getTrmname()) : "";
        if (!termName.isBlank()) {
            return name + " (" + termName + ")";
        }
        return name;
    }

    private String buildStudentName(Students student) {
        if (student == null) {
            return "";
        }
        return String.join(" ", List.of(
                        safeText(student.getStdfirstname()),
                        safeText(student.getStdmiddlename()),
                        safeText(student.getStdlastname())))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String formatCreditHours(BigDecimal credits, BigDecimal lectures,
                                     BigDecimal tutorials, BigDecimal practicals) {
        return formatDecimal(credits) + " (" + formatDecimal(lectures) + " + "
                + formatDecimal(tutorials) + " + " + formatDecimal(practicals) + ")";
    }

    private String preferNumeric(String textValue, BigDecimal numericValue) {
        if (textValue != null && !textValue.isBlank()) {
            return textValue;
        }
        return numericValue != null ? formatDecimal(numericValue) : "--";
    }

    private String formatDecimal(BigDecimal value) {
        return safeDecimal(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String safeText(Object value) {
        return safeText(value, "");
    }

    private String safeText(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString();
        return text.isBlank() ? fallback : text;
    }
}
