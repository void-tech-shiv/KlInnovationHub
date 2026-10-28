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

import com.klu.dto.CollaborationSubmissionRequest;
import com.klu.dto.PublicCollaborationDto;
import com.klu.model.Collaboration;
import com.klu.service.CurrentUserService;
import com.klu.service.implementation.CollaborationImple;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/collaboration")
public class CollaborationController {

    @Autowired
    CollaborationImple collaborationService;
    
    @Autowired
    CurrentUserService currentUser;

    @PostMapping("/create")
    public String createTeam(@Valid @RequestBody CollaborationSubmissionRequest request) {
    	long studentId = currentUser.getCurrentStudent().getStudentId();
    	collaborationService.CreateTeam(request.toEntity(),studentId);
        return "Collboration Posted Sucessfully";
    }
    @GetMapping("/all")
    public List<PublicCollaborationDto> getAllCollaboration() {
        return collaborationService.getAllCollaboration().stream().map(PublicCollaborationDto::from).toList();
    }
    
    @GetMapping("/student/{id}")
    public List<PublicCollaborationDto> getMyTeams(
            @PathVariable Long id){

        return collaborationService.getMyTeams(id).stream().map(PublicCollaborationDto::from).toList();
    }
    
    @DeleteMapping("/delete/{collaborationId}")
    public String deleteTeam( @PathVariable Integer collaborationId) {
        collaborationService.deleteTeam(collaborationId);
        return "Team Deleted Successfully";
    }
    
    @GetMapping("/following")
    public List<PublicCollaborationDto> getFollowingCollaborations() {
        return collaborationService.getFollowingCollaborations().stream().map(PublicCollaborationDto::from).toList();
    }
}