package com.klu.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klu.model.GroupProject;
import com.klu.model.GroupProjectLikes;
import com.klu.model.Project;
import com.klu.model.ProjectLikes;
import com.klu.model.Student;

class PublicProjectResponseTests {
    private final ObjectMapper mapper = new ObjectMapper();

    private Student makeStudent(long id) {
        Student student = new Student();
        student.setStudentId(id);
        student.setStudent_name("Test User");
        student.setStudentEmail("hidden@example.invalid");
        student.setInterestedDomain("Cybersecurity");
        return student;
    }

    @Test
    void publicProjectDoesNotExposePrivateDataOrOtherLikers() {
        Project project = new Project();
        project.setStudent(makeStudent(20));
        ProjectLikes like = new ProjectLikes();
        like.setStudent(makeStudent(30));
        project.setLikes(List.of(like));

        JsonNode guest = mapper.valueToTree(PublicProjectDto.from(project, null));
        assertFalse(guest.path("student").has("studentEmail"));
        assertFalse(guest.path("student").has("interestedDomain"));
        assertFalse(guest.has("likes"));
        assertEquals(1, guest.path("likeCount").asInt());
        assertFalse(guest.path("isLiked").asBoolean());
        assertTrue(mapper.valueToTree(PublicProjectDto.from(project, 30L))
                .path("isLiked").asBoolean());
    }

    @Test
    void publicGroupProjectProtectsTeamMembers() {
        GroupProject project = new GroupProject();
        project.setTeamLead(makeStudent(20));
        project.setStudentList(List.of(makeStudent(30)));
        GroupProjectLikes like = new GroupProjectLikes();
        like.setStudent(makeStudent(30));
        project.setLikes(List.of(like));
        JsonNode dto = mapper.valueToTree(PublicGroupProjectDto.from(project, 30L));
        assertFalse(dto.path("teamLead").has("studentEmail"));
        assertFalse(dto.path("studentList").get(0).has("studentEmail"));
        assertFalse(dto.has("likes"));
        assertEquals(1, dto.path("likeCount").asInt());
        assertTrue(dto.path("isLiked").asBoolean());
    }

    @Test
    void publicProfileOnlyContainsWhitelistedFields() {
        JsonNode dto = mapper.valueToTree(PublicStudentDto.from(makeStudent(20)));
        assertEquals(20, dto.path("studentId").asLong());
        assertFalse(dto.has("studentEmail"));
        assertFalse(dto.has("interestedDomain"));
        assertFalse(dto.has("lastProjectSubmissionAt"));
    }
}
