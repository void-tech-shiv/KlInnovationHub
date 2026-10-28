package com.klu.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.klu.dto.PublicGroupProjectDto;
import com.klu.dto.PublicProjectDto;
import com.klu.model.GroupProject;
import com.klu.model.Project;
import com.klu.model.Student;
import com.klu.repository.StudentRepo;

/**
 * Builds allowlisted API responses. The optional liked-by-me flag is computed
 * server-side; other students' like identities are never sent to browsers.
 */
@Component
public class PublicProjectResponseMapper {
    @Autowired
    private StudentRepo studentRepo;

    public List<PublicProjectDto> projects(List<Project> projects) {
        Long viewerId = currentStudentIdOrNull();
        return projects.stream().map(project -> PublicProjectDto.from(project, viewerId)).toList();
    }

    public List<PublicGroupProjectDto> groupProjects(List<GroupProject> projects) {
        Long viewerId = currentStudentIdOrNull();
        return projects.stream().map(project -> PublicGroupProjectDto.from(project, viewerId)).toList();
    }

    private Long currentStudentIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                        .noneMatch(role -> "ROLE_STUDENT".equals(role.getAuthority()))) {
            return null;
        }
        Student student = studentRepo.findByStudentEmail(authentication.getName());
        return student == null ? null : student.getStudentId();
    }
}
