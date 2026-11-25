package io.github.juste120.docmint.demo.controller;

import io.github.juste120.docmint.demo.dto.request.AttestationRequest;
import io.github.juste120.docmint.demo.dto.response.AttestationResponse;
import io.github.juste120.docmint.demo.dto.response.EmployeeSummaryResponse;
import io.github.juste120.docmint.demo.service.EmployeeService;
import io.github.juste120.docmint.spring.web.Report;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Employees: JSON list and PDF work attestation. */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  @GetMapping
  public List<EmployeeSummaryResponse> list() {
    return service.findAll();
  }

  @GetMapping("/{id}/attestation")
  @Report(value = "attestation", filename = "attestation-{registrationNumber}.pdf")
  public AttestationResponse attestation(@PathVariable Long id, @Valid AttestationRequest request) {
    return service.attestation(id, request);
  }
}
