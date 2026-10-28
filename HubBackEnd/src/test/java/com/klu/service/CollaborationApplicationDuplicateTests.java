package com.klu.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import com.klu.exception.ConflictException;
import com.klu.exception.GlobalExceptionHandler;
import com.klu.model.CollabApplication;
import com.klu.model.Collaboration;
import com.klu.model.Student;
import com.klu.repository.CollabApplicationRepo;
import com.klu.repository.CollaborationRepo;
import com.klu.service.implementation.CollabApplicationImple;

import jakarta.persistence.Table;

@ExtendWith(MockitoExtension.class)
class CollaborationApplicationDuplicateTests {

    @Mock
    private CollabApplicationRepo applicationRepo;

    @Mock
    private CollaborationRepo collaborationRepo;

    @Mock
    private CurrentUserService currentUser;

    @Mock
    private NotificationService notifications;

    @InjectMocks
    private CollabApplicationImple service;

    private Student student(long id) {
        Student student = new Student();
        student.setStudentId(id);
        return student;
    }

    private Collaboration team(long ownerId, Boolean acceptingApplications) {
        Collaboration team = new Collaboration();
        team.setCollaboration_id(123);
        team.setStudent(student(ownerId));
        team.setName("Demo Team");
        team.setStatus(acceptingApplications);
        return team;
    }

    @Test
    void firstApplicationCreatesOneRecordAndOneNotification() {
        Student applicant = student(20);
        Collaboration collaboration = team(10, true);
        when(currentUser.getCurrentStudent()).thenReturn(applicant);
        when(collaborationRepo.findByIdForApplication(123)).thenReturn(Optional.of(collaboration));

        assertEquals("Application Submitted", service.createCollabApplication(123));

        ArgumentCaptor<CollabApplication> saved = ArgumentCaptor.forClass(CollabApplication.class);
        verify(applicationRepo, times(1)).saveAndFlush(saved.capture());
        assertEquals(20L, saved.getValue().getStudent().getStudentId().longValue());
        assertEquals(123, saved.getValue().getCollaboration().getCollaboration_id().intValue());
        assertEquals("PENDING", saved.getValue().getStatus());
        verify(applicationRepo).existsForTeamAndStudent(123, 20L);
        verify(notifications).createNotification(
                collaboration.getStudent(), applicant, "TEAM_APPLICATION", "Demo Team");
    }

    @Test
    void duplicateApplicationReturnsConflictWithoutSavingOrNotifying() {
        when(currentUser.getCurrentStudent()).thenReturn(student(20));
        when(collaborationRepo.findByIdForApplication(123)).thenReturn(Optional.of(team(10, true)));
        when(applicationRepo.existsForTeamAndStudent(123, 20L)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.createCollabApplication(123));

        assertEquals("You have already applied to this team", ex.getMessage());
        verify(applicationRepo, never()).saveAndFlush(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void cannotApplyToOwnTeam() {
        when(currentUser.getCurrentStudent()).thenReturn(student(10));
        when(collaborationRepo.findByIdForApplication(123)).thenReturn(Optional.of(team(10, true)));

        assertThrows(ConflictException.class, () -> service.createCollabApplication(123));

        verifyNoInteractions(applicationRepo, notifications);
    }

    @Test
    void closedTeamsRejectNewApplications() {
        when(currentUser.getCurrentStudent()).thenReturn(student(20));
        when(collaborationRepo.findByIdForApplication(123)).thenReturn(Optional.of(team(10, false)));

        assertThrows(ConflictException.class, () -> service.createCollabApplication(123));

        verifyNoInteractions(applicationRepo, notifications);
    }

    @Test
    void constraintRaceMapsToConflictAndSkipsNotification() {
        when(currentUser.getCurrentStudent()).thenReturn(student(20));
        when(collaborationRepo.findByIdForApplication(123)).thenReturn(Optional.of(team(10, true)));
        when(applicationRepo.saveAndFlush(any(CollabApplication.class)))
                .thenThrow(new DataIntegrityViolationException("Unique index collision"));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.createCollabApplication(123));

        assertTrue(ex.getMessage().contains("conflicts"));
        verifyNoInteractions(notifications);
    }

    @Test
    void entityDeclaresUniqueTeamAndStudentPair() {
        Table table = CollabApplication.class.getAnnotation(Table.class);
        assertTrue(List.of(table.uniqueConstraints()).stream().anyMatch(
                constraint -> constraint.name().equals("uk_collab_application_team_student")
                        && List.of(constraint.columnNames()).containsAll(
                                List.of("collaboration_id", "studentId"))));
    }

    @Test
    void conflictHandlerReturnsHttp409() {
        var response = new GlobalExceptionHandler().handleConflict(
                new ConflictException("You have already applied to this team"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("You have already applied to this team",
                response.getBody().get("message"));
    }
}
