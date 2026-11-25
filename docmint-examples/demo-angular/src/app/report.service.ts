import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

/** URL of the demo API (see docmint-examples/demo-api). */
export const API_URL = 'http://localhost:8080/api';

/** A downloaded document: the PDF and the file name chosen by the server. */
export interface PdfDocument {
  blob: Blob;
  fileName: string;
}

/**
 * Calls DocMint endpoints. A PDF is binary: `responseType: 'blob'` is mandatory, and
 * `observe: 'response'` gives access to the Content-Disposition header (file name).
 * With CORS, the API must expose that header (see WebConfig in the demo API).
 */
@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);

  /** Fetches a PDF from a relative API path, e.g. `invoices/1/pdf`. */
  fetch(path: string): Observable<PdfDocument> {
    return this.http
      .get(`${API_URL}/${path}`, { responseType: 'blob', observe: 'response' })
      .pipe(map((response) => this.toDocument(response, path)));
  }

  /** Opens the PDF in a new browser tab. */
  view(document: PdfDocument): void {
    const url = URL.createObjectURL(document.blob);
    window.open(url, '_blank', 'noopener');
    // Give the new tab time to load the blob before releasing it.
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }

  /** Saves the PDF with the server-provided file name. */
  download(document: PdfDocument): void {
    const url = URL.createObjectURL(document.blob);
    const link = window.document.createElement('a');
    link.href = url;
    link.download = document.fileName;
    link.click();
    URL.revokeObjectURL(url);
  }

  private toDocument(response: HttpResponse<Blob>, path: string): PdfDocument {
    const header = response.headers.get('Content-Disposition') ?? '';
    return {
      blob: response.body ?? new Blob([], { type: 'application/pdf' }),
      fileName: ReportService.fileName(header) ?? `${path.split('/')[0]}.pdf`,
    };
  }

  /** Reads `filename*=UTF-8''...` (preferred, RFC 6266) or `filename="..."`. */
  static fileName(contentDisposition: string): string | null {
    const encoded = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
    if (encoded) {
      return decodeURIComponent(encoded[1]);
    }
    const plain = /filename="([^"]+)"/i.exec(contentDisposition);
    return plain ? plain[1] : null;
  }
}
