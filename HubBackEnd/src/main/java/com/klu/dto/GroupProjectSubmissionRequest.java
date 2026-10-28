package com.klu.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.klu.model.GroupProject;
import com.klu.model.Student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupProjectSubmissionRequest(
        String project_name,
        List<@Valid StudentReference> studentList,
        String description,
        @NotBlank @Pattern(regexp = "^(https://)?(www[.])?github[.]com/.*$",
                message = "Must be a valid GitHub URL") String githubUrl,
        String liveUrl,
        String choice,
        String tech1,
        String tech2,
        String tech3) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StudentReference(@NotNull Long studentId) {}

    public GroupProject toEntity() {
        GroupProject project = new GroupProject();
        project.setProject_name(project_name);
        project.setDescription(description);
        project.setGithubUrl(githubUrl);
        project.setLiveUrl(liveUrl);
        project.setChoice(choice);
        project.setTech1(tech1);
        project.setTech2(tech2);
        project.setTech3(tech3);
        if (studentList != null) {
            project.setStudentList(studentList.stream().map(member -> {
                Student student = new Student();
                student.setStudentId(member.studentId());
                return student;
            }).toList());
        }
        return project;
    }
}
