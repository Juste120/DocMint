package io.github.juste120.agbale.demo.service.impl;

import io.github.juste120.agbale.demo.dto.request.AttestationRequest;
import io.github.juste120.agbale.demo.dto.response.AttestationResponse;
import io.github.juste120.agbale.demo.dto.response.EmployeeSummaryResponse;
import io.github.juste120.agbale.demo.mapper.EmployeeMapper;
import io.github.juste120.agbale.demo.repository.EmployeeRepository;
import io.github.juste120.agbale.demo.service.EmployeeService;
import io.github.juste120.agbale.demo.service.NotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class EmployeeServiceImpl implements EmployeeService {

  private static final String DEFAULT_PURPOSE = "servir et valoir ce que de droit";

  private final EmployeeRepository repository;
  private final EmployeeMapper mapper;
  private final Clock clock;

  EmployeeServiceImpl(EmployeeRepository repository, EmployeeMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
    this.clock = Clock.systemDefaultZone();
  }

  @Override
  public List<EmployeeSummaryResponse> findAll() {
    return repository.findAll().stream().map(mapper::toSummary).toList();
  }

  @Override
  public AttestationResponse attestation(Long id, AttestationRequest request) {
    String purpose =
        request == null || request.purpose() == null || request.purpose().isBlank()
            ? DEFAULT_PURPOSE
            : request.purpose().strip();
    return repository
        .findById(id)
        .map(employee -> mapper.toAttestation(employee, purpose, LocalDate.now(clock)))
        .orElseThrow(() -> new NotFoundException("Employee", id));
  }
}
