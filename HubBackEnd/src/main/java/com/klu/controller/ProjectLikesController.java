package com.klu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.dto.ProjectLikeDto;
import com.klu.dto.PublicProjectDto;
import com.klu.model.Project;
import com.klu.service.CurrentUserService;
import com.klu.service.PublicProjectResponseMapper;
import com.klu.service.implementation.ProjectLikesImple;

@RestController
@RequestMapping("/likes")
public class ProjectLikesController {

    @Autowired
    ProjectLikesImple projectLikesService;

    @Autowired
    PublicProjectResponseMapper publicMapper;
    
    @Autowired
    CurrentUserService currentUser;

    @GetMapping("/top")
    public List<PublicProjectDto> getTopProjects() {
        return publicMapper.projects(projectLikesService.getTopProjects());
    }
    
    @GetMapping("/top3")
    public List<PublicProjectDto> getTop3Projects() {
        return publicMapper.projects(projectLikesService.getTop3Projects());
    }
    
    @GetMapping("/top8")
    public List<PublicProjectDto> getTop8Projects() {
        return publicMapper.projects(projectLikesService.getTop8Projects());
    }
    
    @PostMapping("/toggleLike/{projectId}")
    public ProjectLikeDto toggleLike(@PathVariable Integer projectId) {
    	long studentId = currentUser.getCurrentStudent().getStudentId();
    	return projectLikesService.toggleLike(studentId, projectId);
	}
    
}