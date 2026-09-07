package com.ecampus.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecampus.model.CoursePreferences;
import com.ecampus.model.RegistrationOpenFor;
import com.ecampus.model.SlotPreferences;
import com.ecampus.model.StudentCourseRequirements;
import com.ecampus.model.Students;
import com.ecampus.repository.CoursePreferencesRepository;
import com.ecampus.repository.RegistrationOpenForRepository;
import com.ecampus.repository.SemestersRepository;
import com.ecampus.repository.SlotPreferencesRepository;
import com.ecampus.repository.StudentCourseRequirementsRepository;
import com.ecampus.repository.TermCoursesRepository;

@Service
public class ElectiveRegistrationService {

    private final StudentRegistrationService registrationService;
    private final SemestersRepository semestersRepo;
    private final RegistrationOpenForRepository registrationOpenForRepo;
    private final TermCoursesRepository termCoursesRepo;
    private final CoursePreferencesRepository coursePrefRepo;
    private final SlotPreferencesRepository slotPrefRepo;
    private final StudentCourseRequirementsRepository studCourseReqRepo;

    public ElectiveRegistrationService(StudentRegistrationService registrationService,
                                       SemestersRepository semestersRepo,
                                       RegistrationOpenForRepository registrationOpenForRepo,
                                       TermCoursesRepository termCoursesRepo,
                                       CoursePreferencesRepository coursePrefRepo,
                                       SlotPreferencesRepository slotPrefRepo,
                                       StudentCourseRequirementsRepository studCourseReqRepo) {
        this.registrationService = registrationService;
        this.semestersRepo = semestersRepo;
        this.registrationOpenForRepo = registrationOpenForRepo;
        this.termCoursesRepo = termCoursesRepo;
        this.coursePrefRepo = coursePrefRepo;
        this.slotPrefRepo = slotPrefRepo;
        this.studCourseReqRepo = studCourseReqRepo;
    }

    public ElectiveRegistrationPageData buildRegistrationPage(Long studentId) {
        requireStudentId(studentId);
        Students student = registrationService.getStudentById(studentId);
        if (student == null) {
            throw new ElectiveRegistrationNotFoundException("Student record was not found.");
        }
        Long batchId = student.getStdbchid();
        Long termId = semestersRepo.findMaxTrmIdByBchId(batchId);
        Long semesterId = registrationService.getMaxSemesterId(batchId);

        validateRegistrationWindow(termId, batchId);

        List<Object[]> courses = termCoursesRepo.findCoursesBySlot(termId, "ELECTIVE", batchId);
        List<String> electiveTypes = courses.stream()
                .map(row -> (String) row[6])
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Object, List<Object[]>> slotMap = courses.stream()
                .collect(Collectors.groupingBy(row -> row[7]));

        boolean hasSubmittedPreferences = !coursePrefRepo.getBySid(studentId).isEmpty();
        return new ElectiveRegistrationPageData(semesterId, slotMap, electiveTypes, hasSubmittedPreferences);
    }

    @Transactional
    public void submitPreferences(Long studentId, Map<String, String> allParams) {
        requireStudentId(studentId);
        ParsedPreferences parsed = parsePreferences(allParams);

        List<StudentCourseRequirements> existingRequirements = studCourseReqRepo.findBySid(studentId);
        if (!existingRequirements.isEmpty()) {
            coursePrefRepo.deleteBySid(studentId);
            slotPrefRepo.deleteBySid(studentId);
            studCourseReqRepo.deleteBySid(studentId);
        }

        studCourseReqRepo.saveAll(buildStudentCourseRequirements(studentId, parsed.electiveRequirements()));
        slotPrefRepo.saveAll(buildSlotPreferences(studentId, parsed.slotPriorities()));
        coursePrefRepo.saveAll(buildCoursePreferences(studentId, parsed.coursePreferencesBySlot()));
    }

    public ElectiveRegistrationViewData buildRegistrationView(Long studentId) {
        requireStudentId(studentId);

        List<Object[]> requirements = studCourseReqRepo.getBySid(studentId);
        List<Object[]> slotPreferences = slotPrefRepo.getBySid(studentId);
        List<CoursePreferences> coursePreferences = coursePrefRepo.getBySid(studentId);

        if (requirements == null || requirements.isEmpty()
                || slotPreferences == null || slotPreferences.isEmpty()
                || coursePreferences == null || coursePreferences.isEmpty()) {
            throw new ElectiveRegistrationNotFoundException("No Elective Registration record found.");
        }

        Map<Long, List<CoursePreferences>> groupedCoursePreferences = coursePreferences.stream()
                .collect(Collectors.groupingBy(
                        CoursePreferences::getSlot,
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            list.sort(Comparator.comparing(CoursePreferences::getPrefIndex));
                            return list;
                        })
                ));

        slotPreferences.sort(Comparator.comparing(row ->
                row[1] == null ? Long.MAX_VALUE : ((Number) row[1]).longValue()));

        return new ElectiveRegistrationViewData(groupedCoursePreferences, slotPreferences, requirements);
    }

    private void validateRegistrationWindow(Long termId, Long batchId) {
        RegistrationOpenFor registrationWindow =
                registrationOpenForRepo.getRofByTrmBch(termId, batchId, "Elective");

        LocalDateTime now = LocalDateTime.now();
        if (registrationWindow == null || now.isBefore(registrationWindow.getStartdate())) {
            throw new ElectiveRegistrationClosedException("Elective Registration is not yet open.");
        }
        if (now.isAfter(registrationWindow.getEnddate())) {
            throw new ElectiveRegistrationClosedException("Elective Registration has ended.");
        }
    }

    private ParsedPreferences parsePreferences(Map<String, String> allParams) {
        Map<Long, Map<Long, Long>> coursePreferencesBySlot = new HashMap<>();
        Map<Long, Long> slotPriorities = new HashMap<>();
        Map<String, Long> electiveRequirements = new HashMap<>();

        allParams.forEach((key, value) -> {
            if (value == null || value.trim().isEmpty()) {
                return;
            }

            try {
                if (key.startsWith("pref_")) {
                    String[] parts = key.split("_");
                    Long slotNo = Long.parseLong(parts[1]);
                    Long tcrId = Long.parseLong(parts[2]);
                    Long rank = Long.parseLong(value);
                    coursePreferencesBySlot.computeIfAbsent(slotNo, ignored -> new TreeMap<>()).put(rank, tcrId);
                } else if (key.startsWith("slotPriority_")) {
                    Long slotNo = Long.parseLong(key.split("_")[1]);
                    slotPriorities.put(slotNo, Long.parseLong(value));
                } else if (key.startsWith("count_")) {
                    String type = key.replace("count_", "");
                    electiveRequirements.put(type, Long.parseLong(value));
                }
            } catch (RuntimeException ignored) {
                // Keep the old controller behavior: malformed optional inputs are skipped.
            }
        });

        return new ParsedPreferences(coursePreferencesBySlot, slotPriorities, electiveRequirements);
    }

    private List<StudentCourseRequirements> buildStudentCourseRequirements(
            Long studentId, Map<String, Long> electiveRequirements) {
        List<StudentCourseRequirements> requirements = new ArrayList<>();
        for (Map.Entry<String, Long> entry : electiveRequirements.entrySet()) {
            StudentCourseRequirements requirement = new StudentCourseRequirements();
            requirement.setSid(studentId);
            requirement.setElectType(entry.getKey());
            requirement.setCount(entry.getValue());
            requirements.add(requirement);
        }
        return requirements;
    }

    private List<SlotPreferences> buildSlotPreferences(Long studentId, Map<Long, Long> slotPriorities) {
        List<SlotPreferences> preferences = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : slotPriorities.entrySet()) {
            SlotPreferences slotPreference = new SlotPreferences();
            slotPreference.setSid(studentId);
            slotPreference.setSlot(entry.getKey());
            slotPreference.setPrefIndex(entry.getValue());
            preferences.add(slotPreference);
        }
        return preferences;
    }

    private List<CoursePreferences> buildCoursePreferences(
            Long studentId, Map<Long, Map<Long, Long>> coursePreferencesBySlot) {
        List<CoursePreferences> preferences = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Long>> slotEntry : coursePreferencesBySlot.entrySet()) {
            Long slot = slotEntry.getKey();
            for (Map.Entry<Long, Long> prefEntry : slotEntry.getValue().entrySet()) {
                CoursePreferences coursePreference = new CoursePreferences();
                coursePreference.setSid(studentId);
                coursePreference.setSlot(slot);
                coursePreference.setPrefIndex(prefEntry.getKey());
                coursePreference.setTcrid(prefEntry.getValue());
                preferences.add(coursePreference);
            }
        }
        return preferences;
    }

    private void requireStudentId(Long studentId) {
        if (studentId == null) {
            throw new ElectiveRegistrationNotFoundException("Student record is not linked with this session.");
        }
    }

    private record ParsedPreferences(Map<Long, Map<Long, Long>> coursePreferencesBySlot,
                                     Map<Long, Long> slotPriorities,
                                     Map<String, Long> electiveRequirements) {
    }

    public record ElectiveRegistrationPageData(Long semesterId,
                                               Map<Object, List<Object[]>> slotMap,
                                               List<String> electiveTypes,
                                               boolean hasSubmittedPreferences) {
    }

    public record ElectiveRegistrationViewData(Map<Long, List<CoursePreferences>> groupedCoursePreferences,
                                               List<Object[]> slotPreferences,
                                               List<Object[]> requirements) {
    }

    public static class ElectiveRegistrationClosedException extends RuntimeException {
        public ElectiveRegistrationClosedException(String message) {
            super(message);
        }
    }

    public static class ElectiveRegistrationNotFoundException extends RuntimeException {
        public ElectiveRegistrationNotFoundException(String message) {
            super(message);
        }
    }
}
