import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ReportService } from './report.service';

interface DocumentLink {
  label: string;
  path: string;
}

/** Lists the demo documents with "Voir" (view) and "Télécharger" (download) buttons. */
@Component({
  selector: 'app-documents',
  template: `
    <table>
      <thead>
        <tr><th>Document</th><th></th></tr>
      </thead>
      <tbody>
        @for (doc of documents; track doc.path) {
          <tr>
            <td>{{ doc.label }}</td>
            <td>
              <button type="button" (click)="view(doc)">Voir</button>
              <button type="button" (click)="download(doc)">Télécharger</button>
            </td>
          </tr>
        }
      </tbody>
    </table>
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
  `,
  styles: `
    table { border-collapse: collapse; }
    td, th { padding: 6px 12px; border-bottom: 1px solid #ddd; text-align: left; }
    button { margin-right: 6px; }
    .error { color: #b00020; }
  `,
})
export class DocumentsComponent {
  private readonly reports = inject(ReportService);

  readonly error = signal<string | null>(null);

  readonly documents: DocumentLink[] = [
    { label: 'Facture F-2026-001', path: 'invoices/1/pdf' },
    { label: 'Facture F-2026-002 (plusieurs pages)', path: 'invoices/2/pdf' },
    { label: 'Reçu R-2026-001', path: 'payments/1/receipt' },
    { label: 'Attestation de travail MAT-0042', path: 'employees/1/attestation' },
  ];

  view(doc: DocumentLink): void {
    this.reports.fetch(doc.path).subscribe({
      next: (pdf) => this.reports.view(pdf),
      error: (e: HttpErrorResponse) => this.showError(e),
    });
  }

  download(doc: DocumentLink): void {
    this.reports.fetch(doc.path).subscribe({
      next: (pdf) => this.reports.download(pdf),
      error: (e: HttpErrorResponse) => this.showError(e),
    });
  }

  /** Errors come back as a Blob (responseType 'blob') holding RFC 9457 JSON. */
  private showError(e: HttpErrorResponse): void {
    if (e.error instanceof Blob) {
      e.error.text().then((json) => {
        try {
          const problem = JSON.parse(json) as { code?: string; detail?: string };
          this.error.set(`${problem.code ?? 'ERROR'} : ${problem.detail ?? e.message}`);
        } catch {
          this.error.set(e.message);
        }
      });
    } else {
      this.error.set(e.message);
    }
  }
}
