package com.klu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klu.dto.SocialLinksRequest;
import com.klu.dto.PublicStudentDto;
import com.klu.exception.ForbiddenException;
import com.klu.model.Student;
import com.klu.repository.StudentRepo;
import com.klu.service.CurrentUserService;
import com.klu.service.implementation.StudentImple;

@RestController
@RequestMapping("/student")
public class StudentController {
	
	@Autowired
	StudentImple studentService;
	
	@Autowired
	CurrentUserService currentUser;
	
	@Autowired
	StudentRepo studentRepo;
	
	/*@PostMapping("/create")
	public String create(@RequestParam String email,@RequestParam String name) {
		return studentService.CreateStudentByEmail(email, name);
	}*/
	@GetMapping("/getById/{id}")
	public PublicStudentDto getStudentById(@PathVariable long id) {
        return PublicStudentDto.from(studentService.getStudentById(id));
    }
	@GetMapping("/getByEmail")
	public ResponseEntity<PublicStudentDto> getStudent(@RequestParam String email) {
        Student requester = currentUser.getCurrentStudent();
        if (email == null || !requester.getStudentEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenException("Cannot look up another student's email");
        }
        return ResponseEntity.ok(PublicStudentDto.from(requester));
    }
	
	@PutMapping("/socialLinks")
    public ResponseEntity<Student> updateSocialLinks(@RequestBody SocialLinksRequest request) {
		long studentId = currentUser.getCurrentStudent().getStudentId();
        Student updatedStudent = studentService.updateSocialLinks(studentId, request);
        return ResponseEntity.ok(updatedStudent);
    }
	
	@GetMapping("/exists/{studentId}")
	public ResponseEntity<Boolean> studentExists(@PathVariable Long studentId) {
	    return ResponseEntity.ok(studentRepo.existsById(studentId));
	}
	
	
}
