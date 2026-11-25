package io.github.juste120.docmint.demo.service;

import io.github.juste120.docmint.demo.dto.request.AttestationRequest;
import io.github.juste120.docmint.demo.dto.response.AttestationResponse;
import io.github.juste120.docmint.demo.dto.response.EmployeeSummaryResponse;
import java.util.List;

/** Employee use cases. */
public interface EmployeeService {

  List<EmployeeSummaryResponse> findAll();

  AttestationResponse attestation(Long id, AttestationRequest request);
}
