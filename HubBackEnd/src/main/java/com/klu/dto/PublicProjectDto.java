package com.klu.dto;

import java.util.List;

import com.klu.model.Project;
import com.klu.model.ProjectLikes;
import com.klu.model.ProjectStatus;

/** Public API response: no Student entity and no individual liker identities. */
public record PublicProjectDto(
        Integer projectId,
        PublicStudentDto student,
        String projectName,
        String githubUrl,
        String liveUrl,
        String description,
        String choice,
        String tech1,
        String tech2,
        String tech3,
        ProjectStatus status,
        int likeCount,
        boolean isLiked) {

    public static PublicProjectDto from(Project project, Long viewerStudentId) {
        List<ProjectLikes> likes = project.getLikes();
        int likeCount = likes == null ? 0 : likes.size();
        boolean likedByViewer = viewerStudentId != null && likes != null
                && likes.stream().anyMatch(like -> like.getStudent() != null
                        && viewerStudentId.equals(like.getStudent().getStudentId()));
        return new PublicProjectDto(
                project.getProjectId(),
                PublicStudentDto.from(project.getStudent()),
                project.getProjectName(),
                project.getGithubUrl(),
                project.getLiveUrl(),
                project.getDescription(),
                project.getChoice(),
                project.getTech1(),
                project.getTech2(),
                project.getTech3(),
                project.getStatus(),
                likeCount,
                likedByViewer);
    }
}
