package com.klu.dto;

import com.klu.model.Student;

/**
 * Student fields intended to appear on project cards and public profiles.
 * Never serialize the JPA Student entity into shared project responses.
 */
public record PublicStudentDto(
        Long studentId,
        String student_name,
        Integer year,
        String branch,
        String avatarUrl,
        String githubUrl,
        String linkedinUrl) {

    public static PublicStudentDto from(Student student) {
        if (student == null) {
            return null;
        }
        return new PublicStudentDto(
                student.getStudentId(),
                student.getStudent_name(),
                student.getYear(),
                student.getBranch(),
                student.getAvatarUrl(),
                student.getGithubUrl(),
                student.getLinkedinUrl());
    }
}
