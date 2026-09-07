package com.ecampus.controller.student;

import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecampus.config.RegistrationDeadlineConfig;
import com.ecampus.dto.OverallCourseTypeProgressDTO;
import com.ecampus.model.Batches;
import com.ecampus.model.Programs;
import com.ecampus.model.Semesters;
import com.ecampus.model.StudentRegistrations;
import com.ecampus.model.StudentSemesterResult;
import com.ecampus.model.Students;
import com.ecampus.model.Users;
import com.ecampus.repository.ProgramsRepository;
import com.ecampus.repository.StudentSemesterResultRepository;
import com.ecampus.repository.UserRepository;
import com.ecampus.session.SessionConstants;
import com.ecampus.service.StudentGraduationRequirementsService;
import com.ecampus.service.StudentRegistrationService;
import com.ecampus.util.LoggedUser;
import com.ecampus.util.RomanNumeralUtil;
import com.ecampus.util.UnAuthorisedUserException;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student")
public class StudentDashboardController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepo;

        @Autowired
        private StudentRegistrationService registrationService;

        @Autowired
        private StudentGraduationRequirementsService graduationRequirementsService;

        @Autowired
        private StudentSemesterResultRepository studentSemesterResultRepo;

        @Autowired
        private ProgramsRepository programsRepo;

        @Autowired
        private RegistrationDeadlineConfig deadlineConfig;

    @GetMapping("/dashboard")
        public String dashboard(HttpSession session, Model model) {
        Long studentId = resolveStudentId(session);
        if (studentId == null) {
            model.addAttribute("error", "Unable to resolve student account");
            return "student/student-dashboard";
        }

        Students student = registrationService.getStudentById(studentId);
        if (student == null) {
            model.addAttribute("error", "Student record not found");
            return "student/student-dashboard";
        }

        String studentName = buildStudentName(student);
        String studentPhotoPath = normalizePhotoPath(student.getStdphotolocation());

        Batches batch = null;
        Programs program = null;
        if (student.getStdbchid() != null) {
            batch = graduationRequirementsService.getBatch(student.getStdbchid());
        }
        if (batch != null && batch.getBchprgid() != null) {
            program = programsRepo.getprgId(batch.getBchprgid());
        }

        List<Semesters> semesters = student.getStdbchid() == null
            ? List.of()
            : registrationService.getSemesterById(student.getStdbchid());
        List<StudentRegistrations> regs = registrationService.getRegistrationsByStudentId(studentId);
        Long latestSemesterId = getLatestRegisteredSemesterId(semesters, regs);
        if (latestSemesterId == null && student.getStdbchid() != null) {
            latestSemesterId = registrationService.getMaxSemesterId(student.getStdbchid());
        }
        Long resolvedSemesterId = latestSemesterId;
        Semesters latestSemester = semesters.stream()
            .filter(sem -> sem.getStrid().equals(resolvedSemesterId))
            .findFirst()
            .orElse(null);

        String currentSemesterLabel = buildSemesterLabel(latestSemester);
        String currentTermLabel = buildTermLabel(latestSemester);

        StudentSemesterResult latestResult = studentSemesterResultRepo.findLatestByStudentId(studentId);
        String currentCpi = latestResult != null && latestResult.getSsrcpi() != null
            ? latestResult.getSsrcpi()
            : "--";
        String currentSpi = latestResult != null && latestResult.getSsrspi() != null
            ? latestResult.getSsrspi()
            : "--";

        List<OverallCourseTypeProgressDTO> overallProgress = List.of();
        BigDecimal totalCompletedCredits = BigDecimal.ZERO;
        BigDecimal totalRequiredCredits = BigDecimal.ZERO;
        BigDecimal totalRemainingCredits = BigDecimal.ZERO;
        if (batch != null) {
            overallProgress = graduationRequirementsService
                .buildOverallProgress(studentId, batch.getSchemeId(), batch.getSplid());

            BigDecimal totalExtraCredits = overallProgress.stream()
                .map(OverallCourseTypeProgressDTO::getExtraCredits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalCompletedCredits = overallProgress.stream()
                .map(OverallCourseTypeProgressDTO::getCompletedCredits)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(totalExtraCredits);
            totalRequiredCredits = overallProgress.stream()
                .map(OverallCourseTypeProgressDTO::getMinCredits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalRemainingCredits = totalRequiredCredits.subtract(totalCompletedCredits).max(BigDecimal.ZERO);
        }

        boolean registrationOpen = latestSemesterId != null && deadlineConfig.isWithinDeadline();
        StudentRegistrations latestRegistration = latestSemesterId == null
            ? null
            : registrationService.getExistingRegistration(studentId, latestSemesterId);
        String registrationActionLabel = resolveRegistrationAction(registrationOpen, latestRegistration);
        String registrationStatusText = registrationOpen
            ? "Registrations are now open"
            : "Registrations are currently closed";

        String registrationActionUrl = latestSemesterId != null
            ? "/student/registration/edit?strid=" + latestSemesterId
            : null;

        LocalDate today = LocalDate.now();
        CalendarView calendar = buildCalendarView(today);

        model.addAttribute("studentName", studentName);
        model.addAttribute("studentInstId", student.getStdinstid());
        model.addAttribute("programmeName", program != null ? program.getPrgname() : "Programme");
        model.addAttribute("batchName", batch != null ? batch.getBchname() : "");
        model.addAttribute("studentPhotoPath", studentPhotoPath);

        model.addAttribute("currentSemesterLabel", currentSemesterLabel);
        model.addAttribute("currentTermLabel", currentTermLabel);
        model.addAttribute("currentCpi", currentCpi);
        model.addAttribute("currentSpi", currentSpi);

        model.addAttribute("overallProgressList", overallProgress);
        model.addAttribute("totalCompletedCredits", formatDecimal(totalCompletedCredits));
        model.addAttribute("totalRequiredCredits", formatDecimal(totalRequiredCredits));
        model.addAttribute("totalRemainingCredits", formatDecimal(totalRemainingCredits));

        model.addAttribute("registrationStatusText", registrationStatusText);
        model.addAttribute("registrationOpen", registrationOpen);
        model.addAttribute("registrationActionLabel", registrationActionLabel);
        model.addAttribute("registrationActionUrl", registrationActionUrl);
        model.addAttribute("registrationSemesterLabel", currentSemesterLabel);
        model.addAttribute("registrationTermLabel", currentTermLabel);

        model.addAttribute("calendarMonthLabel", calendar.monthLabel());
        model.addAttribute("calendarWeeks", calendar.weeks());

        return "student/student-dashboard";
    }

    @GetMapping("/change-password")
    public String changePassword() {
        return "student/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$";

        // Check match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/student/change-password";
        }

        // Check strength
        if (!newPassword.matches(pattern)) {
            redirectAttributes.addFlashAttribute("error",
                    "Password must have 8+ chars, uppercase, lowercase, number and special character");
            return "redirect:/student/change-password";
        }

        // Encrypt password using BCrypt
        String encodedPassword = passwordEncoder.encode(newPassword);

        LoggedUser loggedUser = currentLoggedUser(session);
        Users user = userRepo.findByUname(loggedUser.getUserName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Save password
        user.setPassword(encodedPassword);
        userRepo.save(user);

        redirectAttributes.addFlashAttribute("success", "Password changed successfully");

        return "redirect:/student/change-password";
    }

    private Long resolveStudentId(HttpSession session) {
        LoggedUser user = currentLoggedUser(session);
        Long studentId = user.getStdId();
        String instituteId = user.getUnivId();
        if (instituteId != null && !instituteId.isBlank()) {
            Long latestStudentId = registrationService.getLatestStudentIdByInstituteId(instituteId);
            if (latestStudentId != null) {
                studentId = latestStudentId;
            }
        }

        String username = user.getUserName();
        if (username != null && username.matches("\\d+")) {
            Long mappedByUsername = registrationService.getLatestStudentIdByInstituteId(username);
            if (mappedByUsername != null) {
                studentId = mappedByUsername;
            }
        }

        return studentId;
    }

    private LoggedUser currentLoggedUser(HttpSession session) {
        if (session == null) {
            throw new UnAuthorisedUserException();
        }

        LoggedUser user = (LoggedUser) session.getAttribute(SessionConstants.CURRENT_USER);
        if (user == null) {
            throw new UnAuthorisedUserException();
        }

        return user;
    }

    private String buildStudentName(Students student) {
        String first = student.getStdfirstname() == null ? "" : student.getStdfirstname();
        String middle = student.getStdmiddlename() == null ? "" : student.getStdmiddlename();
        String last = student.getStdlastname() == null ? "" : student.getStdlastname();
        return String.join(" ", List.of(first, middle, last)).trim().replaceAll("\\s+", " ");
    }

    private Long getLatestRegisteredSemesterId(List<Semesters> semesters, List<StudentRegistrations> regs) {
        if (semesters == null || semesters.isEmpty() || regs == null || regs.isEmpty()) {
            return null;
        }

        Map<Long, Long> seqBySemesterId = semesters.stream()
                .collect(Collectors.toMap(
                        Semesters::getStrid,
                        s -> s.getStrseqno() == null ? 0L : s.getStrseqno(),
                        (a, b) -> a));

        return regs.stream()
                .map(StudentRegistrations::getSrgstrid)
                .filter(seqBySemesterId::containsKey)
                .max(Comparator.comparingLong(id -> seqBySemesterId.getOrDefault(id, 0L)))
                .orElse(null);
    }

    private String buildSemesterLabel(Semesters semester) {
        if (semester == null) {
            return "--";
        }
        Long seq = semester.getStrseqno();
        if (seq != null && seq > 0 && seq <= 3999) {
            return "Sem " + RomanNumeralUtil.toRoman(seq);
        }
        return semester.getStrname() != null ? semester.getStrname() : "--";
    }

    private String buildTermLabel(Semesters semester) {
        if (semester == null || semester.getTerms() == null) {
            return "";
        }
        String termName = semester.getTerms().getTrmname();
        String year = semester.getTerms().getAcademicYear() != null
                ? semester.getTerms().getAcademicYear().getAyrname()
                : "";
        if (termName == null) {
            return year;
        }
        if (year == null || year.isBlank()) {
            return termName;
        }
        return year + " " + termName;
    }

    private String resolveRegistrationAction(boolean registrationOpen, StudentRegistrations latestRegistration) {
        if (!registrationOpen && latestRegistration == null) {
            return "View";
        }
        if (registrationOpen && latestRegistration == null) {
            return "Register Now";
        }
        return registrationOpen ? "Edit" : "View";
    }

    private String normalizePhotoPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return "/images/students/default-avatar.svg";
        }
        return storedPath.trim();
    }

    private CalendarView buildCalendarView(LocalDate today) {
        YearMonth month = YearMonth.from(today);
        String monthLabel = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + month.getYear();

        List<List<CalendarDay>> weeks = new ArrayList<>();
        LocalDate firstDay = month.atDay(1);
        int firstWeekdayIndex = normalizeWeekdayIndex(firstDay.getDayOfWeek());
        int daysInMonth = month.lengthOfMonth();

        int dayCounter = 1;
        List<CalendarDay> week = new ArrayList<>();

        for (int i = 0; i < firstWeekdayIndex; i++) {
            week.add(new CalendarDay(null, false));
        }

        while (dayCounter <= daysInMonth) {
            while (week.size() < 7 && dayCounter <= daysInMonth) {
                boolean isToday = today.getDayOfMonth() == dayCounter;
                week.add(new CalendarDay(dayCounter, isToday));
                dayCounter++;
            }
            weeks.add(week);
            week = new ArrayList<>();
        }

        if (!week.isEmpty()) {
            while (week.size() < 7) {
                week.add(new CalendarDay(null, false));
            }
            weeks.add(week);
        }

        return new CalendarView(monthLabel, weeks);
    }

    private int normalizeWeekdayIndex(DayOfWeek dayOfWeek) {
        int javaIndex = dayOfWeek.getValue();
        return javaIndex % 7;
    }

    private String formatDecimal(BigDecimal value) {
        BigDecimal normalized = (value != null ? value : BigDecimal.ZERO).stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }

    private record CalendarDay(Integer day, boolean today) {
    }

    private record CalendarView(String monthLabel, List<List<CalendarDay>> weeks) {
    }
}
