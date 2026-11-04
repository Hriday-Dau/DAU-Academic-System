package com.ecampus.dto.master;

import java.util.ArrayList;
import java.util.List;

public class StudentImportResult {

    private int totalRows;
    private int successCount;
    private int errorCount;
    private List<String> errors = new ArrayList<>();
    private List<String> successStudents = new ArrayList<>();

    public StudentImportResult() {}

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getSuccessStudents() {
        return successStudents;
    }

    public void setSuccessStudents(List<String> successStudents) {
        this.successStudents = successStudents;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public void addSuccess(String student) {
        this.successStudents.add(student);
    }
}

