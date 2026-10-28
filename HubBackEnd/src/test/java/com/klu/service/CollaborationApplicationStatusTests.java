package com.klu.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.klu.exception.ConflictException;
import com.klu.exception.ForbiddenException;
import com.klu.exception.GlobalExceptionHandler;
import com.klu.exception.ResourceNotFoundException;
import com.klu.model.CollabApplication;
import com.klu.model.CollabApplicationStatus;
import com.klu.model.Collaboration;
import com.klu.model.Student;
import com.klu.repository.CollabApplicationRepo;
import com.klu.service.implementation.CollabApplicationImple;

@ExtendWith(MockitoExtension.class)
class CollaborationApplicationStatusTests {

    @Mock private CollabApplicationRepo applicationRepo;
    @Mock private CurrentUserService currentUser;
    @InjectMocks private CollabApplicationImple service;

    private Student student(long id) {
        Student value = new Student();
        value.setStudentId(id);
        return value;
    }

    private CollabApplication application(String status) {
        Collaboration team = new Collaboration();
        team.setStudent(student(10));
        CollabApplication app = new CollabApplication();
        app.setCollabApplication_id(15);
        app.setCollaboration(team);
        app.setStudent(student(20));
        app.setStatus(status);
        return app;
    }

    @Test
    void acceptsOnlyTerminalDecisionsAndNormalizesCase() {
        assertEquals(CollabApplicationStatus.ACCEPTED,
                CollabApplicationStatus.parseOwnerDecision(" accepted "));
        assertEquals(CollabApplicationStatus.REJECTED,
                CollabApplicationStatus.parseOwnerDecision("rejected"));
        for (String invalid : new String[] {
                "PENDING", "APPROVED", "DELETED", "WAITING", "ACCEPTED_NOW", ""
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> CollabApplicationStatus.parseOwnerDecision(invalid));
        }
        assertThrows(IllegalArgumentException.class,
                () -> CollabApplicationStatus.parseOwnerDecision(null));
    }

    @Test
    void invalidStatusReturns400WithoutQueryingApplication() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateApplicationStatus(15, "APPROVED"));
        var response = new GlobalExceptionHandler().handleBadRequest(error);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(applicationRepo, currentUser);
    }

    @Test
    void ownerMayAcceptPendingApplicationOnce() {
        CollabApplication app = application("PENDING");
        when(applicationRepo.findByIdForStatusUpdate(15)).thenReturn(Optional.of(app));
        when(currentUser.getCurrentStudent()).thenReturn(student(10));

        assertEquals("Application status updated to ACCEPTED",
                service.updateApplicationStatus(15, " accepted "));

        assertEquals("ACCEPTED", app.getStatus());
        verify(applicationRepo).save(app);
    }

    @Test
    void ownerMayRejectPendingApplicationOnce() {
        CollabApplication app = application("PENDING");
        when(applicationRepo.findByIdForStatusUpdate(15)).thenReturn(Optional.of(app));
        when(currentUser.getCurrentStudent()).thenReturn(student(10));

        assertEquals("Application status updated to REJECTED",
                service.updateApplicationStatus(15, "rejected"));

        assertEquals("REJECTED", app.getStatus());
        verify(applicationRepo).save(app);
    }

    @Test
    void unauthorizedStudentCannotChangeStatus() {
        CollabApplication app = application("PENDING");
        when(applicationRepo.findByIdForStatusUpdate(15)).thenReturn(Optional.of(app));
        when(currentUser.getCurrentStudent()).thenReturn(student(99));

        assertThrows(ForbiddenException.class,
                () -> service.updateApplicationStatus(15, "ACCEPTED"));

        assertEquals("PENDING", app.getStatus());
        verify(applicationRepo, never()).save(any());
    }

    @Test
    void acceptedAndRejectedStatesCannotBeOverwrittenOrReplayed() {
        for (String state : new String[] {"ACCEPTED", "REJECTED"}) {
            clearInvocations(applicationRepo, currentUser);
            CollabApplication app = application(state);
            when(applicationRepo.findByIdForStatusUpdate(15)).thenReturn(Optional.of(app));
            when(currentUser.getCurrentStudent()).thenReturn(student(10));

            ConflictException error = assertThrows(ConflictException.class,
                    () -> service.updateApplicationStatus(15, "ACCEPTED"));

            assertEquals(state, app.getStatus());
            assertEquals(HttpStatus.CONFLICT,
                    new GlobalExceptionHandler().handleConflict(error).getStatusCode());
            verify(applicationRepo, never()).save(any());
        }
    }

    @Test
    void missingApplicationReturns404() {
        when(applicationRepo.findByIdForStatusUpdate(15)).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> service.updateApplicationStatus(15, "REJECTED"));

        assertEquals(HttpStatus.NOT_FOUND,
                new GlobalExceptionHandler().handleNotFound(error).getStatusCode());
        verifyNoInteractions(currentUser);
    }

    @Test
    void secondDecisionOnSameApplicationIsRejected() {
        CollabApplication app = application("PENDING");
        when(applicationRepo.findByIdForStatusUpdate(15)).thenReturn(Optional.of(app));
        when(currentUser.getCurrentStudent()).thenReturn(student(10));

        assertEquals("Application status updated to ACCEPTED",
                service.updateApplicationStatus(15, "ACCEPTED"));
        assertThrows(ConflictException.class,
                () -> service.updateApplicationStatus(15, "REJECTED"));

        assertEquals("ACCEPTED", app.getStatus());
        verify(applicationRepo, times(1)).save(app);
    }

    @Test
    void nullApplicationIdIsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateApplicationStatus(null, "ACCEPTED"));
        verifyNoInteractions(applicationRepo, currentUser);
    }
}
