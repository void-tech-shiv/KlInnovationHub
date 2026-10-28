package com.klu.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klu.model.CollabApplication;
import com.klu.model.Collaboration;
import com.klu.model.Student;

class PublicCollaborationPrivacyTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nestedApplicantAndTeamOwnerNeverContainSensitiveStudentFields() {
        Student owner = student(10);
        Student applicant = student(25);
        Collaboration team = new Collaboration();
        team.setCollaboration_id(100);
        team.setName("Team");
        team.setStudent(owner);
        CollabApplication application = new CollabApplication();
        application.setCollaboration(team);
        application.setStudent(applicant);
        application.setStatus("PENDING");

        JsonNode result = mapper.valueToTree(PublicCollabApplicationDto.from(application));
        JsonNode applicantData = result.path("student");
        JsonNode ownerData = result.path("collaboration").path("student");

        assertEquals(25L, applicantData.path("studentId").asLong());
        assertEquals(10L, ownerData.path("studentId").asLong());
        assertEquals(100, result.path("collaboration").path("collaboration_id").asInt());
        assertEquals("PENDING", result.path("status").asText());
        assertFalse(applicantData.has("studentEmail"));
        assertFalse(ownerData.has("studentEmail"));
        assertFalse(applicantData.has("lastProjectSubmissionAt"));
        assertFalse(ownerData.has("interestedDomain"));
    }

    private Student student(long id) {
        Student student = new Student();
        student.setStudentId(id);
        student.setStudent_name("Example");
        student.setStudentEmail("private@example.invalid");
        student.setInterestedDomain("AI/ML");
        return student;
    }
}
