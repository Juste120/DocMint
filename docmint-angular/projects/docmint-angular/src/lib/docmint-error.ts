import { HttpErrorResponse } from '@angular/common/http';

/** An error returned by a DocMint endpoint (RFC 9457 Problem Details). */
export class DocMintError extends Error {
  constructor(
    /** HTTP status, e.g. 500 or 406 (0 for network errors). */
    readonly status: number,
    /** Stable DocMint code, e.g. `DOCMINT_TEMPLATE_NOT_FOUND`, or `HTTP_ERROR`. */
    readonly code: string,
    /** Short title. */
    readonly title: string,
    /** Actionable message. */
    readonly detail: string,
  ) {
    super(`${code}: ${detail}`);
    this.name = 'DocMintError';
  }
}

interface Problem {
  code?: unknown;
  title?: unknown;
  detail?: unknown;
}

/**
 * Converts an HTTP error into a {@link DocMintError}.
 *
 * With `responseType: 'blob'`, Angular gives the error body as a `Blob`: it is read and parsed as
 * RFC 9457 JSON when possible.
 */
export async function toDocMintError(error: unknown): Promise<DocMintError> {
  if (!(error instanceof HttpErrorResponse)) {
    return new DocMintError(0, 'CLIENT_ERROR', 'Client error', String(error));
  }
  let body: unknown = error.error;
  if (typeof Blob !== 'undefined' && body instanceof Blob) {
    body = await body.text();
  }
  if (typeof body === 'string') {
    try {
      body = JSON.parse(body);
    } catch {
      body = { detail: body };
    }
  }
  const problem = (body && typeof body === 'object' ? body : {}) as Problem;
  return new DocMintError(
    error.status,
    typeof problem.code === 'string' ? problem.code : 'HTTP_ERROR',
    typeof problem.title === 'string' ? problem.title : error.statusText || 'Error',
    typeof problem.detail === 'string' ? problem.detail : error.message,
  );
}
