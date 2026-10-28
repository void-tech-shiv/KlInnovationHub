package com.klu.service;

import java.util.List;

import com.klu.dto.PublicCollabApplicationDto;

public interface CollabApplicationService {

	String createCollabApplication(Integer collaborationId);
	List<PublicCollabApplicationDto> getApplicationsForOwnedTeams();
	List<PublicCollabApplicationDto> getMyApplications(Long studentid);
	String updateApplicationStatus(Integer applicationId, String status);
}
