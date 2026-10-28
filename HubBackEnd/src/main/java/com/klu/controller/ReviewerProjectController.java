package com.klu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.dto.PublicGroupProjectDto;
import com.klu.dto.PublicProjectDto;
import com.klu.model.GroupProject;
import com.klu.model.Project;
import com.klu.model.Reviewer;
import com.klu.service.CurrentReviewerService;
import com.klu.service.PublicProjectResponseMapper;
import com.klu.service.implementation.GroupProjectImple;
import com.klu.service.implementation.ProjectImple;

@RestController
@RequestMapping("/reviewer")
public class ReviewerProjectController {

    @Autowired private ProjectImple projectService;
    @Autowired private GroupProjectImple groupProjectService;
    @Autowired private CurrentReviewerService currentReviewerService;
    @Autowired private PublicProjectResponseMapper publicMapper;

    @GetMapping("/projects/pending")
    public List<PublicProjectDto> getPendingProjects() {
        return publicMapper.projects(projectService.getPendingProjects());
    }

    @GetMapping("/groupProjects/pending")
    public List<PublicGroupProjectDto> getPendingGroupProjects() {
        return publicMapper.groupProjects(groupProjectService.getPendingGroupProjects());
    }

    @GetMapping("/projects/recommended")
    public List<PublicProjectDto> getRecommendedProjects() {
        Reviewer reviewer = currentReviewerService.getCurrentReviewer();
        return publicMapper.projects(projectService.getRecommendedProjects(reviewer.getChoice1(), reviewer.getChoice2(), reviewer.getChoice3()));
    }

    @GetMapping("/groupProjects/recommended")
    public List<PublicGroupProjectDto> getRecommendedGroupProjects() {
        Reviewer reviewer = currentReviewerService.getCurrentReviewer();
        return publicMapper.groupProjects(groupProjectService.getRecommendedGroupProjects(reviewer.getChoice1(), reviewer.getChoice2(), reviewer.getChoice3()));
    }
}
