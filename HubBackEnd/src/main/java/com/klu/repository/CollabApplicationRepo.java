package com.klu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.klu.model.CollabApplication;

@Repository
public interface CollabApplicationRepo extends JpaRepository<CollabApplication,Integer>{

	List<CollabApplication> findByStudent_StudentId(Long studentId);

    // Applications received by teams owned by the current student.
    List<CollabApplication> findByCollaboration_Student_StudentId(Long ownerId);

}
