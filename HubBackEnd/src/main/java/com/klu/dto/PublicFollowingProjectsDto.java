package com.klu.dto;

import java.util.List;

/** Response for the following feed without exposing arbitrary Student entities. */
public record PublicFollowingProjectsDto(
        List<PublicProjectDto> projects,
        List<PublicGroupProjectDto> groupProjects) {
}
