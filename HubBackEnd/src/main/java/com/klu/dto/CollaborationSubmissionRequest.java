package com.klu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.klu.model.Collaboration;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CollaborationSubmissionRequest(
        String name,
        String problemStatement,
        String description,
        Integer teamSize,
        String skill1,
        String skill2,
        String skill3,
        String linkedIn) {

    public Collaboration toEntity() {
        Collaboration team = new Collaboration();
        team.setName(name);
        team.setProblemStatement(problemStatement);
        team.setDescription(description);
        team.setTeamSize(teamSize);
        team.setSkill1(skill1);
        team.setSkill2(skill2);
        team.setSkill3(skill3);
        team.setLinkedIn(linkedIn);
        team.setStatus(true);
        return team;
    }
}
