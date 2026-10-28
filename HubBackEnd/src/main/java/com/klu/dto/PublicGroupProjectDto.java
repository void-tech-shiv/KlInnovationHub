package com.klu.dto;

import java.util.List;

import com.klu.model.GroupProject;
import com.klu.model.GroupProjectLikes;
import com.klu.model.ProjectStatus;

/** Group project response with only the member fields displayed by the UI. */
public record PublicGroupProjectDto(
        Integer groupProjectId,
        String project_name,
        List<PublicStudentDto> studentList,
        String description,
        String githubUrl,
        String liveUrl,
        PublicStudentDto teamLead,
        String choice,
        String tech1,
        String tech2,
        String tech3,
        ProjectStatus status,
        int likeCount,
        boolean isLiked) {

    public static PublicGroupProjectDto from(GroupProject project, Long viewerStudentId) {
        List<GroupProjectLikes> likes = project.getLikes();
        int likeCount = likes == null ? 0 : likes.size();
        boolean likedByViewer = viewerStudentId != null && likes != null
                && likes.stream().anyMatch(like -> like.getStudent() != null
                        && viewerStudentId.equals(like.getStudent().getStudentId()));
        return new PublicGroupProjectDto(
                project.getGroupProjectId(),
                project.getProject_name(),
                project.getStudentList() == null ? List.of()
                        : project.getStudentList().stream().map(PublicStudentDto::from).toList(),
                project.getDescription(),
                project.getGithubUrl(),
                project.getLiveUrl(),
                PublicStudentDto.from(project.getTeamLead()),
                project.getChoice(),
                project.getTech1(),
                project.getTech2(),
                project.getTech3(),
                project.getStatus(),
                likeCount,
                likedByViewer);
    }
}
