 package com.klu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "collabApplication", uniqueConstraints = @UniqueConstraint(
        name = "uk_collab_application_team_student",
        columnNames = {"collaboration_id", "studentId"}))
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollabApplication {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer collabApplication_id;
	
	@ManyToOne
	@JoinColumn(name="collaboration_id")
	private Collaboration collaboration;
	

	@ManyToOne
	@JoinColumn(name="studentId")
	private Student student;
	
	private String status = "PENDING";

}
