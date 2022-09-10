package com.marcelocastro.service;

import com.marcelocastro.dto.StudentDto;
import com.marcelocastro.entity.StudentEntity;
import com.marcelocastro.repository.StudentRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StudentService {

  private StudentRepository studentRepository;

  public List<StudentEntity> listAllStudents() {
    return StreamSupport.stream(studentRepository.findAll().spliterator(), false).collect(Collectors.toList());
  }

  public StudentEntity getStudentById(String id) {
    return studentRepository.findById(Long.valueOf(id))
        .orElseThrow(() -> new IllegalArgumentException("No student found with provided Id"));
  }
}
