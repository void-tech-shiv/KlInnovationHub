package com.klu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import com.klu.model.CollabApplication;

@Repository
public interface CollabApplicationRepo extends JpaRepository<CollabApplication,Integer>{

	List<CollabApplication> findByStudent_StudentId(Long studentId);

    @Query("""
           select (count(a) > 0) from CollabApplication a
           where a.collaboration.collaboration_id = :teamId
             and a.student.studentId = :studentId
           """)
    boolean existsForTeamAndStudent(
            @Param("teamId") Integer teamId,
            @Param("studentId") Long studentId);

    /**
     * Lock an individual application row until the current transaction ends,
     * preventing two owners' concurrent decisions from overwriting each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CollabApplication a where a.collabApplication_id = :applicationId")
    Optional<CollabApplication> findByIdForStatusUpdate(
            @Param("applicationId") Integer applicationId);

    // Applications received by teams owned by the current student.
    List<CollabApplication> findByCollaboration_Student_StudentId(Long ownerId);

}
