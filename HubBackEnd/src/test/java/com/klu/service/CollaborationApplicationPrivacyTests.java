package com.klu.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.klu.dto.PublicCollabApplicationDto;
import com.klu.exception.ForbiddenException;
import com.klu.model.CollabApplication;
import com.klu.model.Collaboration;
import com.klu.model.Student;
import com.klu.repository.CollabApplicationRepo;
import com.klu.service.implementation.CollabApplicationImple;

@ExtendWith(MockitoExtension.class)
class CollaborationApplicationPrivacyTests {
    @Mock
    private CollabApplicationRepo repository;
    @Mock
    private CurrentUserService currentUser;
    @InjectMocks
    private CollabApplicationImple service;

    private Student student(long id) {
        Student student = new Student();
        student.setStudentId(id);
        student.setStudentEmail("private@example.invalid");
        student.setStudent_name("Student " + id);
        return student;
    }

    private CollabApplication application(long ownerId, long applicantId) {
        Collaboration collaboration = new Collaboration();
        collaboration.setCollaboration_id(101);
        collaboration.setStudent(student(ownerId));
        collaboration.setName("Example Team");
        CollabApplication application = new CollabApplication();
        application.setCollabApplication_id(7);
        application.setCollaboration(collaboration);
        application.setStudent(student(applicantId));
        application.setStatus("PENDING");
        return application;
    }

    @Test
    void teamOwnerOnlyReceivesApplicationsForOwnedTeams() {
        when(currentUser.getCurrentStudent()).thenReturn(student(10));
        when(repository.findByCollaboration_Student_StudentId(10L))
                .thenReturn(List.of(application(10, 25)));

        List<PublicCollabApplicationDto> result = service.getApplicationsForOwnedTeams();

        assertEquals(1, result.size());
        assertEquals(25L, result.get(0).student().studentId());
        assertEquals(101, result.get(0).collaboration().collaboration_id());
        verify(repository).findByCollaboration_Student_StudentId(10L);
        verify(repository, never()).findAll();
    }

    @Test
    void applicantCanReadOnlyTheirOwnApplications() {
        when(currentUser.getCurrentStudent()).thenReturn(student(25));
        when(repository.findByStudent_StudentId(25L))
                .thenReturn(List.of(application(10, 25)));

        assertEquals(1, service.getMyApplications(25L).size());
        verify(repository).findByStudent_StudentId(25L);
    }

    @Test
    void anotherStudentsApplicationsAreForbiddenWithoutQueryingRepository() {
        when(currentUser.getCurrentStudent()).thenReturn(student(25));

        assertThrows(ForbiddenException.class, () -> service.getMyApplications(99L));

        verifyNoInteractions(repository);
    }
}
