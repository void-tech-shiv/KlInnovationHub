package com.klu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.dto.ProjectSubmissionRequest;
import com.klu.dto.PublicProjectDto;
import com.klu.model.Project;
import com.klu.service.CurrentUserService;
import com.klu.service.PublicProjectResponseMapper;
import com.klu.service.implementation.ProjectImple;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/project")
public class ProjectController {
	
	@Autowired
	ProjectImple projectService;

    @Autowired
    PublicProjectResponseMapper publicMapper;
	
	@Autowired
	CurrentUserService currentUser;
	
	@PostMapping("/submit")
    public String submitProject(@Valid @RequestBody ProjectSubmissionRequest request) {
		long id = currentUser.getCurrentStudent().getStudentId();
        return projectService.SubmitProject(request.toEntity(),id);
    }
    @GetMapping("/latest")
    public List<PublicProjectDto> getLatestSubmissions() {
        return publicMapper.projects(projectService.getLatestSubmissions());
    }
    @GetMapping("/all")
    public List<PublicProjectDto> getAllProjects() {
        return publicMapper.projects(projectService.getAllProjects());
    }
    @GetMapping("/year/{year}")
    public List<PublicProjectDto> getProjectsByYear(@PathVariable Integer year) {
        return publicMapper.projects(projectService.getProjectsByYear(year));
    }
    @GetMapping("/branch/{bname}")
    public List<PublicProjectDto> getProjectsByBranch(@PathVariable String bname) {
        return publicMapper.projects(projectService.getProjectsByBranch(bname));
    }
    
    @GetMapping("/branch/{branch}/year/{year}")
    public List<PublicProjectDto> getProjectsByBranchAndYear(@PathVariable String branch,@PathVariable Integer year) {
    	return publicMapper.projects(projectService.getProjectsByBranchAndYear(branch, year));
	}
    
    @GetMapping("/student/{id}")
    public List<PublicProjectDto> getProjectsByStudentId(@PathVariable long id) {
        return publicMapper.projects(projectService.getProjectsByid(id));
    }
    
    @DeleteMapping("/deleteProject/{projectId}")
    public String deleteProject(@PathVariable int projectId) {
    	return projectService.deleteProjectsById(projectId);
    }
    
    @GetMapping("/my-pending")
    public List<PublicProjectDto> getMyPendingProjects() {
        return publicMapper.projects(projectService.getMyPendingProjects());
    }
}
	

