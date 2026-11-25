import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, from, map, switchMap, throwError } from 'rxjs';
import { toDocMintError } from './docmint-error';
import { parseFileName } from './file-name';

/** A document downloaded from a DocMint endpoint. */
export interface DocMintDocument {
  /** The document content. */
  readonly blob: Blob;
  /** The file name chosen by the server (`Content-Disposition`), or a fallback. */
  readonly fileName: string;
  /** The media type, e.g. `application/pdf` or `image/png`. */
  readonly mediaType: string;
}

/**
 * Calls DocMint endpoints (`@Report`) and views, downloads or previews the result.
 *
 * ```ts
 * private readonly docs = inject(DocMintClient);
 * this.docs.view('/api/invoices/1/pdf').subscribe();
 * this.docs.download('/api/invoices/1/pdf').subscribe();
 * ```
 *
 * Errors are emitted as {@link DocMintError} (`code`, `title`, `detail`). With CORS, the API must
 * expose `Content-Disposition`, otherwise the browser hides the file name.
 */
@Injectable({ providedIn: 'root' })
export class DocMintClient {
  constructor(private readonly http: HttpClient) {}

  /** Downloads a document without showing it. */
  fetch(url: string): Observable<DocMintDocument> {
    return this.http.get(url, { responseType: 'blob', observe: 'response' }).pipe(
      map((response) => toDocument(response, url)),
      catchError((error: unknown) =>
        from(toDocMintError(error)).pipe(switchMap((e) => throwError(() => e))),
      ),
    );
  }

  /** Opens the document in a new browser tab. */
  view(url: string): Observable<DocMintDocument> {
    return this.fetch(url).pipe(
      map((document) => {
        const objectUrl = URL.createObjectURL(document.blob);
        window.open(objectUrl, '_blank', 'noopener');
        // Give the new tab time to load the document before releasing it.
        setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
        return document;
      }),
    );
  }

  /** Saves the document with the server's file name (or `fileName` if given). */
  download(url: string, fileName?: string): Observable<DocMintDocument> {
    return this.fetch(url).pipe(
      map((document) => {
        const objectUrl = URL.createObjectURL(document.blob);
        const link = window.document.createElement('a');
        link.href = objectUrl;
        link.download = fileName ?? document.fileName;
        link.click();
        URL.revokeObjectURL(objectUrl);
        return document;
      }),
    );
  }

  /**
   * Returns an object URL for an image preview (`@Report(format = PNG)`), to bind to `<img [src]>`.
   * Call {@link release} when the image is no longer displayed.
   */
  preview(url: string): Observable<string> {
    return this.fetch(url).pipe(map((document) => URL.createObjectURL(document.blob)));
  }

  /** Releases an object URL returned by {@link preview}. */
  release(objectUrl: string): void {
    URL.revokeObjectURL(objectUrl);
  }
}

function toDocument(response: HttpResponse<Blob>, url: string): DocMintDocument {
  const mediaType =
    response.headers.get('Content-Type') ?? response.body?.type ?? 'application/octet-stream';
  const extension = mediaType.includes('png') ? 'png' : 'pdf';
  const fallback = `${url.split('?')[0].split('/').filter(Boolean).slice(-2, -1)[0] ?? 'document'}.${extension}`;
  return {
    blob: response.body ?? new Blob([], { type: mediaType }),
    fileName: parseFileName(response.headers.get('Content-Disposition')) ?? fallback,
    mediaType,
  };
}
