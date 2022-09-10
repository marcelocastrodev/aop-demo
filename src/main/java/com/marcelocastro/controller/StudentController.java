package com.marcelocastro.controller;

import com.marcelocastro.dto.StudentDto;
import com.marcelocastro.service.StudentService;
import com.marcelocastro.util.Domain;
import com.marcelocastro.util.Hashids;
import com.marcelocastro.util.LogRequestResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@LogRequestResponse
@AllArgsConstructor
public class StudentController {

  private StudentService studentService;

  @GetMapping
  public List<StudentDto> listAllStudents() {
    return studentService.listAllStudents()
        .stream()
        .map(StudentDto::fromEntity)
        .collect(Collectors.toList());
  }

  @GetMapping("/{id}")
  public StudentDto getStudentById(@PathVariable @Hashids(domain = Domain.STUDENT) String id) {
    return StudentDto.fromEntity(studentService.getStudentById(id));
  }
}
