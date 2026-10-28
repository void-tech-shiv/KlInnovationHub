package com.klu.service.implementation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klu.dto.PublicCollabApplicationDto;
import com.klu.exception.ForbiddenException;
import com.klu.exception.ConflictException;
import com.klu.exception.ResourceNotFoundException;
import com.klu.model.CollabApplication;
import com.klu.model.CollabApplicationStatus;
import com.klu.model.Collaboration;
import com.klu.model.Student;
import com.klu.repository.CollabApplicationRepo;
import com.klu.repository.CollaborationRepo;
import com.klu.service.CollabApplicationService;
import com.klu.service.CurrentUserService;
import com.klu.service.NotificationService;

@Service
public class CollabApplicationImple implements CollabApplicationService {

	@Autowired
	CollabApplicationRepo collabApplicationRepo;
	
	@Autowired 
	CurrentUserService currentUser;
	
	@Autowired
	CollaborationRepo collaborationRepo;
	
	@Autowired
	NotificationService notificationService;
	
    @Override
    @Transactional
    public String createCollabApplication(Integer collaborationId) {
        if (collaborationId == null) {
            throw new IllegalArgumentException("A team must be selected");
        }

        Student applicant = currentUser.getCurrentStudent();
        // Serialize submissions for the same team, including across backend
        // instances using the same database. Keep the lock until commit.
        Collaboration team = collaborationRepo.findByIdForApplication(collaborationId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        if (team.getStudent().getStudentId().equals(applicant.getStudentId())) {
            throw new ConflictException("You cannot apply to your own team");
        }
        if (!Boolean.TRUE.equals(team.getStatus())) {
            throw new ConflictException("This team is not accepting applications");
        }
        if (collabApplicationRepo.existsForTeamAndStudent(
                collaborationId, applicant.getStudentId())) {
            throw new ConflictException("You have already applied to this team");
        }

        CollabApplication application = new CollabApplication();
        application.setCollaboration(team);
        application.setStudent(applicant);
        application.setStatus("PENDING");

        try {
            // Flush before notifying the owner. The database uniqueness
            // constraint is a second safeguard against racing inserts.
            collabApplicationRepo.saveAndFlush(application);
        } catch (DataIntegrityViolationException duplicateOrInvalidRecord) {
            throw new ConflictException("Application conflicts with an existing record");
        }

        notificationService.createNotification(
                team.getStudent(), applicant, "TEAM_APPLICATION", team.getName());
        return "Application Submitted";
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicCollabApplicationDto> getApplicationsForOwnedTeams() {
        Long ownerId = currentUser.getCurrentStudent().getStudentId();
        return collabApplicationRepo.findByCollaboration_Student_StudentId(ownerId)
                .stream()
                .map(PublicCollabApplicationDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicCollabApplicationDto> getMyApplications(Long studentId) {
        Long currentStudentId = currentUser.getCurrentStudent().getStudentId();
        if (studentId == null || !currentStudentId.equals(studentId)) {
            throw new ForbiddenException("Cannot view another student's applications");
        }
        return collabApplicationRepo.findByStudent_StudentId(currentStudentId)
                .stream()
                .map(PublicCollabApplicationDto::from)
                .toList();
    }

    @Override
    @Transactional
    public String updateApplicationStatus(Integer applicationId, String requestedStatus) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID is required");
        }
        // Reject unsupported values, including PENDING, before touching the DB.
        CollabApplicationStatus decision =
                CollabApplicationStatus.parseOwnerDecision(requestedStatus);

        // Row lock + transaction ensures a single transition from PENDING.
        CollabApplication application = collabApplicationRepo
                .findByIdForStatusUpdate(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        Long currentStudentId = currentUser.getCurrentStudent().getStudentId();
        Long ownerId = application.getCollaboration().getStudent().getStudentId();
        if (!currentStudentId.equals(ownerId)) {
            throw new ForbiddenException("Not authorized");
        }

        if (!CollabApplicationStatus.PENDING.name().equals(application.getStatus())) {
            throw new ConflictException(
                    "This application has already been decided and cannot be changed");
        }

        application.setStatus(decision.name());
        collabApplicationRepo.save(application);
        return "Application status updated to " + decision.name();
    }

}
