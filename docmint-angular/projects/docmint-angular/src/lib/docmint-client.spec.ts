import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { firstValueFrom, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DocMintClient } from './docmint-client';
import { DocMintError, toDocMintError } from './docmint-error';

function fakeHttp(result: unknown) {
  return {
    get: vi.fn(() =>
      result instanceof HttpErrorResponse ? throwError(() => result) : of(result),
    ),
  } as never;
}

const pdf = new Blob(['%PDF-1.7'], { type: 'application/pdf' });
const ok = new HttpResponse<Blob>({
  body: pdf,
  status: 200,
  headers: new HttpHeaders({
    'Content-Type': 'application/pdf',
    'Content-Disposition': `inline; filename="facture-F-1.pdf"; filename*=UTF-8''facture-F-1.pdf`,
  }),
});
const problem = new HttpErrorResponse({
  status: 500,
  statusText: 'Internal Server Error',
  error: new Blob(
    [
      JSON.stringify({
        title: 'Template not found',
        detail: "Template 'invoce' not found. Did you mean 'invoice'?",
        code: 'DOCMINT_TEMPLATE_NOT_FOUND',
      }),
    ],
    { type: 'application/problem+json' },
  ),
});

describe('DocMintClient', () => {
  const opened: string[] = [];
  const clicked: { href: string; download: string }[] = [];

  beforeEach(() => {
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:doc'),
      revokeObjectURL: vi.fn(),
    });
    vi.stubGlobal('window', {
      open: vi.fn((url: string) => opened.push(url)),
      document: {
        createElement: () => {
          const link = { href: "", download: "", click: () => clicked.push({ href: link.href, download: link.download }) };
          return link;
        },
      },
    });
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
    opened.length = 0;
    clicked.length = 0;
  });

  it('fetches with blob response and reads the file name', async () => {
    const http = fakeHttp(ok);
    const document = await firstValueFrom(new DocMintClient(http).fetch('/api/invoices/1/pdf'));

    expect(document.fileName).toBe('facture-F-1.pdf');
    expect(document.mediaType).toBe('application/pdf');
    expect((http as { get: ReturnType<typeof vi.fn> }).get).toHaveBeenCalledWith('/api/invoices/1/pdf', {
      responseType: 'blob',
      observe: 'response',
    });
  });

  it('views in a new tab and downloads with the server file name', async () => {
    const client = new DocMintClient(fakeHttp(ok));

    await firstValueFrom(client.view('/api/invoices/1/pdf'));
    expect(opened).toEqual(['blob:doc']);
    vi.runAllTimers();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:doc');

    await firstValueFrom(client.download('/api/invoices/1/pdf'));
    expect(clicked).toEqual([{ href: 'blob:doc', download: 'facture-F-1.pdf' }]);
  });

  it('uses a fallback file name when the header is hidden by CORS', async () => {
    const hidden = new HttpResponse<Blob>({ body: pdf, status: 200 });
    const document = await firstValueFrom(new DocMintClient(fakeHttp(hidden)).fetch('/api/invoices/1/pdf'));

    expect(document.fileName).toBe('1.pdf');
  });

  it('previews and releases images', async () => {
    const client = new DocMintClient(fakeHttp(ok));

    expect(await firstValueFrom(client.preview('/api/invoices/1/preview'))).toBe('blob:doc');
    client.release('blob:doc');
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:doc');
  });

  it('turns RFC 9457 blobs into DocMintError', async () => {
    const error = await firstValueFrom(new DocMintClient(fakeHttp(problem)).fetch('/x')).catch((e) => e);

    expect(error).toBeInstanceOf(DocMintError);
    expect(error.code).toBe('DOCMINT_TEMPLATE_NOT_FOUND');
    expect(error.title).toBe('Template not found');
    expect(error.status).toBe(500);
    expect(error.detail).toContain("Did you mean 'invoice'?");
  });
});

describe('toDocMintError', () => {
  it('handles non-JSON bodies and non-HTTP errors', async () => {
    const plain = await toDocMintError(new HttpErrorResponse({ status: 502, statusText: 'Bad Gateway', error: 'oops' }));
    expect(plain.code).toBe('HTTP_ERROR');
    expect(plain.detail).toBe('oops');

    const other = await toDocMintError(new Error('boom'));
    expect(other.status).toBe(0);
    expect(other.code).toBe('CLIENT_ERROR');
  });
});
