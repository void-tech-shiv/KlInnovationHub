package com.klu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.klu.model.Project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectSubmissionRequest(
        String projectName,
        @NotBlank @Pattern(regexp = "^(https://)?(www[.])?github[.]com/.*$",
                message = "Must be a valid GitHub URL") String githubUrl,
        String liveUrl,
        String description,
        String choice,
        String tech1,
        String tech2,
        String tech3) {

    public Project toEntity() {
        Project project = new Project();
        project.setProjectName(projectName);
        project.setGithubUrl(githubUrl);
        project.setLiveUrl(liveUrl);
        project.setDescription(description);
        project.setChoice(choice);
        project.setTech1(tech1);
        project.setTech2(tech2);
        project.setTech3(tech3);
        return project;
    }
}
