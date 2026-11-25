package io.github.juste120.agbale.demo.mapper;

import io.github.juste120.agbale.demo.dto.response.AttestationResponse;
import io.github.juste120.agbale.demo.dto.response.EmployeeSummaryResponse;
import io.github.juste120.agbale.demo.entity.Employee;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps employees to attestation DTOs. */
@Mapper
public interface EmployeeMapper {

  @Mapping(target = "purpose", source = "purpose")
  @Mapping(target = "issuedOn", source = "issuedOn")
  @Mapping(target = "city", constant = "Lomé")
  AttestationResponse toAttestation(Employee employee, String purpose, LocalDate issuedOn);

  EmployeeSummaryResponse toSummary(Employee employee);
}
