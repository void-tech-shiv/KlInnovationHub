package com.klu.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.dto.GroupProjectLikeDto;
import com.klu.dto.PublicGroupProjectDto;
import com.klu.model.GroupProject;
import com.klu.service.CurrentUserService;
import com.klu.service.PublicProjectResponseMapper;
import com.klu.service.implementation.GroupProjectLikesImple;

@RestController
@RequestMapping("/grouplikes")
public class GroupProjectLikesController {

    @Autowired
    GroupProjectLikesImple groupProjectLikesService;

    @Autowired
    PublicProjectResponseMapper publicMapper;
    
    @Autowired
    CurrentUserService currentUser;

    @GetMapping("/top")
    public List<PublicGroupProjectDto> getTopGroupProjects() {
        return publicMapper.groupProjects(groupProjectLikesService.getTopGroupProjects());
    }
    
    @GetMapping("/top8")
    public List<PublicGroupProjectDto> getTop8Projects() {
        return publicMapper.groupProjects(groupProjectLikesService.getTop8Projects());
    }
    
    @PostMapping("/toggleLike/{projectId}")
    public	GroupProjectLikeDto toggleLike(@PathVariable Integer projectId)	{
    	long studentId = currentUser.getCurrentStudent().getStudentId();
    	return groupProjectLikesService.toggleLike(studentId, projectId);
    	
    }
    
}