package com.klu.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klu.model.Collaboration;
import com.klu.model.GroupProject;
import com.klu.model.Project;

class SubmissionRequestBindingTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void individualProjectIgnoresClientControlledFields() throws Exception {
        String json = """
                {
                  "projectId": 15,
                  "status": "APPROVED",
                  "student": {"studentId": 99},
                  "likes": [{"id": 1}],
                  "projectName": "Demo",
                  "githubUrl": "https://github.com/example/demo"
                }
                """;
        Project project = mapper.readValue(json, ProjectSubmissionRequest.class).toEntity();

        assertNull(project.getProjectId());
        assertNull(project.getStudent());
        assertNull(project.getStatus());
        assertNull(project.getLikes());
        assertEquals("Demo", project.getProjectName());
    }

    @Test
    void groupProjectIgnoresClientControlledFieldsAndMemberMetadata() throws Exception {
        String json = """
                {
                  "groupProjectId": 23,
                  "status": "APPROVED",
                  "teamLead": {"studentId": 99},
                  "project_name": "Team Demo",
                  "githubUrl": "https://github.com/example/team",
                  "studentList": [
                    {"studentId": 42, "studentEmail": "untrusted@example.com"}
                  ]
                }
                """;
        GroupProject project = mapper.readValue(json, GroupProjectSubmissionRequest.class).toEntity();

        assertNull(project.getGroupProjectId());
        assertNull(project.getTeamLead());
        assertNull(project.getStatus());
        assertEquals(1, project.getStudentList().size());
        assertEquals(42L, project.getStudentList().get(0).getStudentId());
        assertNull(project.getStudentList().get(0).getStudentEmail());
    }

    @Test
    void collaborationIgnoresClientOwnershipAndStatus() throws Exception {
        String json = """
                {
                  "collaboration_id": 77,
                  "student": {"studentId": 99},
                  "status": false,
                  "name": "New Team",
                  "teamSize": 3
                }
                """;
        Collaboration team = mapper.readValue(json, CollaborationSubmissionRequest.class).toEntity();

        assertNull(team.getCollaboration_id());
        assertNull(team.getStudent());
        assertTrue(team.getStatus());
        assertEquals("New Team", team.getName());
    }
}
