package com.klu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klu.dto.CollabApplyRequest;
import com.klu.dto.PublicCollabApplicationDto;
import com.klu.service.CurrentUserService;
import com.klu.service.implementation.CollabApplicationImple;



@RestController
@RequestMapping("/collabapplication")
public class CollaborationApplicationController {

    @Autowired
    CollabApplicationImple collabApplicationService;

    @Autowired
    CurrentUserService currentUser;

    @PostMapping("/apply")
    public String createCollabApplication(@RequestBody CollabApplyRequest request) {
        return collabApplicationService.createCollabApplication(request.getCollaborationId());
    }
    
    // Legacy /all route remains valid, but only returns applications for owned teams.
    @GetMapping({"/all", "/owned"})
    public List<PublicCollabApplicationDto> getApplicationsForOwnedTeams() {
        return collabApplicationService.getApplicationsForOwnedTeams();
    }

    @GetMapping("/mine")
    public List<PublicCollabApplicationDto> getCurrentStudentApplications() {
        return collabApplicationService.getMyApplications(
                currentUser.getCurrentStudent().getStudentId());
    }

    // Legacy URL retained; the service rejects access to another user's applications.
    @GetMapping("/student/{id}")
    public List<PublicCollabApplicationDto> getMyApplications(@PathVariable Long id) {
        return collabApplicationService.getMyApplications(id);
    }

    @PatchMapping("/updateStatus/{id}")
    public String updateApplicationStatus(@PathVariable Integer id,@RequestParam String status) {
        return collabApplicationService.updateApplicationStatus(id, status);
    }
    
}