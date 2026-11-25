import { Component, inject, signal } from '@angular/core';
import { DocMintClient, DocMintError } from '@juste120/docmint-angular';

interface DocumentLink {
  label: string;
  path: string;
  preview?: string;
}

/** URL of the demo API (see docmint-examples/demo-api). */
const API_URL = 'http://localhost:8080/api';

/** Lists the demo documents with "Voir", "Télécharger" and "Aperçu" buttons. */
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
              @if (doc.preview) {
                <button type="button" (click)="showPreview(doc)">Aperçu</button>
              }
            </td>
          </tr>
        }
      </tbody>
    </table>
    @if (previewUrl(); as url) {
      <p><img [src]="url" alt="Aperçu" width="300" /></p>
    }
    @if (error(); as e) {
      <p class="error">{{ e.code }} : {{ e.detail }}</p>
    }
  `,
  styles: `
    table { border-collapse: collapse; }
    td, th { padding: 6px 12px; border-bottom: 1px solid #ddd; text-align: left; }
    button { margin-right: 6px; }
    img { border: 1px solid #ccc; }
    .error { color: #b00020; }
  `,
})
export class DocumentsComponent {
  private readonly docs = inject(DocMintClient);

  readonly error = signal<DocMintError | null>(null);
  readonly previewUrl = signal<string | null>(null);

  readonly documents: DocumentLink[] = [
    { label: 'Facture F-2026-001', path: 'invoices/1/pdf', preview: 'invoices/1/preview' },
    { label: 'Facture F-2026-002 (plusieurs pages, non payée)', path: 'invoices/2/pdf' },
    { label: 'Reçu R-2026-001', path: 'payments/1/receipt' },
    { label: 'Attestation de travail MAT-0042 (protégée : MAT-0042)', path: 'employees/1/attestation' },
    { label: 'Relevé des ventes 2026 (graphiques)', path: 'reports/sales/pdf' },
    { label: 'Relevé des ventes 2026 en Excel', path: 'reports/sales/xlsx' },
    { label: 'Liste des factures en Excel', path: 'invoices/export.xlsx' },
    { label: 'Liste des factures en CSV', path: 'invoices/export.csv' },
  ];

  view(doc: DocumentLink): void {
    this.docs.view(`${API_URL}/${doc.path}`).subscribe({ error: (e) => this.error.set(e) });
  }

  download(doc: DocumentLink): void {
    this.docs.download(`${API_URL}/${doc.path}`).subscribe({ error: (e) => this.error.set(e) });
  }

  showPreview(doc: DocumentLink): void {
    this.docs.preview(`${API_URL}/${doc.preview}`).subscribe({
      next: (url) => {
        const previous = this.previewUrl();
        if (previous) {
          this.docs.release(previous);
        }
        this.previewUrl.set(url);
      },
      error: (e) => this.error.set(e),
    });
  }
}
