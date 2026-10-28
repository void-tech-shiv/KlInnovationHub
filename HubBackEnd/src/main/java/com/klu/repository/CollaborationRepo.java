package com.klu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.klu.model.Collaboration;

@Repository
public interface CollaborationRepo extends JpaRepository<Collaboration,Integer>{

	List<Collaboration> findByStudent_StudentId(Long studentId);

    // Lock the team row while checking/submitting an application.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Collaboration c where c.collaboration_id = :teamId")
    Optional<Collaboration> findByIdForApplication(@Param("teamId") Integer teamId);
	
	@Query("""
		    select c
		    from Collaboration c
		    where c.student.studentId in :studentIds
		      and c.status = true
		    order by c.collaboration_id desc
		""")
	List<Collaboration> findByFollowingStudentIds(@Param("studentIds") List<Long> studentIds);
	
}
