package com.marcelocastro.dto;

import com.marcelocastro.entity.StudentEntity;
import com.marcelocastro.util.Domain;
import com.marcelocastro.util.Hasheable;
import com.marcelocastro.util.Hashids;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class StudentDto implements Hasheable {

  @Hashids(domain = Domain.STUDENT)
  private String id;
  private String firstName;
  private String lastName;
  private String email;
  
  public static StudentDto fromEntity(StudentEntity entity) {
    StudentDto dto = new StudentDto();
    BeanUtils.copyProperties(entity, dto);
    dto.setId(entity.getId().toString());
    return dto;
  }
}
