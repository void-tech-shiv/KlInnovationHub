package com.klu.dto;

import com.klu.model.Collaboration;

/**
 * Fields required for collaboration discovery and team ownership views.
 * Does not serialize the Student JPA entity or its private account metadata.
 */
public record PublicCollaborationDto(
        Integer collaboration_id,
        PublicStudentDto student,
        String name,
        String problemStatement,
        String description,
        Integer teamSize,
        String skill1,
        String skill2,
        String skill3,
        String linkedIn,
        Boolean status) {

    public static PublicCollaborationDto from(Collaboration team) {
        if (team == null) {
            return null;
        }
        return new PublicCollaborationDto(
                team.getCollaboration_id(),
                PublicStudentDto.from(team.getStudent()),
                team.getName(),
                team.getProblemStatement(),
                team.getDescription(),
                team.getTeamSize(),
                team.getSkill1(),
                team.getSkill2(),
                team.getSkill3(),
                team.getLinkedIn(),
                team.getStatus());
    }
}
