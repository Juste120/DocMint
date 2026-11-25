import { describe, expect, it } from 'vitest';
import { parseFileName } from './file-name';

describe('parseFileName', () => {
  it('prefers the UTF-8 encoded form', () => {
    expect(
      parseFileName(`inline; filename="re_u.pdf"; filename*=UTF-8''re%C3%A7u-%C3%A9l%C3%A8ve.pdf`),
    ).toBe('reçu-élève.pdf');
  });

  it('reads quoted and bare names', () => {
    expect(parseFileName('attachment; filename="facture-F-1.pdf"')).toBe('facture-F-1.pdf');
    expect(parseFileName('attachment; filename=facture.pdf')).toBe('facture.pdf');
  });

  it('never returns a path', () => {
    expect(parseFileName('attachment; filename="../../etc/passwd"')).toBe('passwd');
    expect(parseFileName(`inline; filename*=UTF-8''..%2F..%2Fx.pdf`)).toBe('x.pdf');
  });

  it('handles missing or malformed headers', () => {
    expect(parseFileName(null)).toBeNull();
    expect(parseFileName('inline')).toBeNull();
    expect(parseFileName(`inline; filename*=UTF-8''%E0%A4%A; filename="a.pdf"`)).toBe('a.pdf');
  });
});
