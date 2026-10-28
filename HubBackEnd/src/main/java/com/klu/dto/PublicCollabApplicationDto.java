package com.klu.dto;

import com.klu.model.CollabApplication;

/**
 * Restricted application response. Includes only display-safe applicant and
 * collaboration fields; never returns raw JPA entities to clients.
 */
public record PublicCollabApplicationDto(
        Integer collabApplication_id,
        PublicCollaborationDto collaboration,
        PublicStudentDto student,
        String status) {

    public static PublicCollabApplicationDto from(CollabApplication application) {
        return new PublicCollabApplicationDto(
                application.getCollabApplication_id(),
                PublicCollaborationDto.from(application.getCollaboration()),
                PublicStudentDto.from(application.getStudent()),
                application.getStatus());
    }
}
