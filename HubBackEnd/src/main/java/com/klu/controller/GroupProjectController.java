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

import com.klu.dto.GroupProjectSubmissionRequest;
import com.klu.dto.PublicGroupProjectDto;
import com.klu.exception.ForbiddenException;
import com.klu.model.GroupProject;
import com.klu.service.CurrentUserService;
import com.klu.service.PublicProjectResponseMapper;
import com.klu.service.implementation.GroupProjectImple;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/groupProject")
public class GroupProjectController {

    @Autowired
    GroupProjectImple groupProjectService;

    @Autowired
    PublicProjectResponseMapper publicMapper;
    
    @Autowired
    CurrentUserService currentUser;

    @PostMapping("/submit/{teamLeadId}")
    public String submitGroupProject(@Valid @RequestBody GroupProjectSubmissionRequest request,@PathVariable Long teamLeadId) {
    	
    	if(!teamLeadId.equals(currentUser.getCurrentStudent().getStudentId())) {
    		throw new ForbiddenException("Not authorized");
    	}
        return groupProjectService.SubmitGroupProject(request.toEntity(),teamLeadId);
    }
    
    @GetMapping("/latest")
    public List<PublicGroupProjectDto> getLatestGroupSubmissions() {
        return publicMapper.groupProjects(groupProjectService.getLatestGroupSubmissions());
    }
    @GetMapping("/all")
    public List<PublicGroupProjectDto> getAllProjects() {
        return publicMapper.groupProjects(groupProjectService.getAllProjects());
    }
    @GetMapping("/year/{year}")
    public List<PublicGroupProjectDto> getProjectsByYear(@PathVariable Integer year) {
        return publicMapper.groupProjects(groupProjectService.getProjectsByYear(year));
    }
    @GetMapping("/branch/{branch}")
    public List<PublicGroupProjectDto> getProjectsByBranch(@PathVariable String branch) {
        return publicMapper.groupProjects(groupProjectService.getProjectsByBranch(branch));
    }
    @GetMapping("/branch/{branch}/year/{year}")
    public List<PublicGroupProjectDto> getProjectsByBranchAndYear(@PathVariable String branch,@PathVariable Integer year) {
        return publicMapper.groupProjects(groupProjectService.getProjectsByBranchAndYear(branch, year));
    }
    @GetMapping("/student/{id}")
    public List<PublicGroupProjectDto> getProjectsById(@PathVariable long id) {
        return publicMapper.groupProjects(groupProjectService.getProjectsByid(id));
    }
    
    @DeleteMapping("/deleteProject/{projectId}")
    public  String deleteProject(@PathVariable int projectId) {
    	return groupProjectService.deleteProjectsById(projectId);
    }
    
    @GetMapping("/my-pending")
    public List<PublicGroupProjectDto> getMyPendingProjects() {
        return publicMapper.groupProjects(groupProjectService.getMyPendingProjects());
    }
}