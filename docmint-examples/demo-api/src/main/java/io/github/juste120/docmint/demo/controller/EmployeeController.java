package io.github.juste120.docmint.demo.controller;

import io.github.juste120.docmint.demo.dto.request.AttestationRequest;
import io.github.juste120.docmint.demo.dto.response.AttestationResponse;
import io.github.juste120.docmint.demo.dto.response.EmployeeSummaryResponse;
import io.github.juste120.docmint.demo.service.EmployeeService;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.PdfStandard;
import io.github.juste120.docmint.domain.Permission;
import io.github.juste120.docmint.domain.Protection;
import io.github.juste120.docmint.domain.SignatureSettings;
import io.github.juste120.docmint.spring.web.Report;
import io.github.juste120.docmint.spring.web.ReportContent;
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
  private final SignatureSettings signature;

  public EmployeeController(EmployeeService service, SignatureSettings signature) {
    this.service = service;
    this.signature = signature;
  }

  @GetMapping
  public List<EmployeeSummaryResponse> list() {
    return service.findAll();
  }

  @GetMapping("/{id}/attestation")
  @Report(value = "attestation", filename = "attestation-{registrationNumber}.pdf")
  public ReportContent attestation(@PathVariable Long id, @Valid AttestationRequest request) {
    AttestationResponse attestation = service.attestation(id, request);
    // Personal document: opening requires the employee's registration number, and it can be
    // printed but not modified.
    return ReportContent.of(attestation)
        .withProtection(
            Protection.openPassword(attestation.registrationNumber().toCharArray())
                .withPermissions(Permission.PRINT, Permission.PRINT_HIGH_QUALITY));
  }

  /** The same attestation as an editable Word document (module docmint-word). */
  @GetMapping("/{id}/attestation.docx")
  @Report(
      value = "attestation",
      filename = "attestation-{registrationNumber}.docx",
      format = OutputFormat.DOCX)
  public AttestationResponse attestationWord(
      @PathVariable Long id, @Valid AttestationRequest request) {
    return service.attestation(id, request);
  }

  /** Official version: PDF/A-3b (archiving), accessible, and electronically signed. */
  @GetMapping("/{id}/attestation-signee.pdf")
  @Report(value = "attestation", filename = "attestation-{registrationNumber}-signee.pdf")
  public ReportContent signedAttestation(@PathVariable Long id, @Valid AttestationRequest request) {
    return ReportContent.of(service.attestation(id, request))
        .withPdfStandard(PdfStandard.PDF_A_3B)
        .withAccessible(true)
        .withSignature(signature);
  }
}
