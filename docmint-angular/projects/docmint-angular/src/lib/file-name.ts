/**
 * Reads the file name from a `Content-Disposition` header.
 *
 * The RFC 6266 / RFC 5987 form `filename*=UTF-8''...` (used by DocMint for non-ASCII names) is
 * preferred over the plain `filename="..."`.
 *
 * @returns the file name, or `null` if the header has none
 */
export function parseFileName(contentDisposition: string | null | undefined): string | null {
  if (!contentDisposition) {
    return null;
  }
  const encoded = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(contentDisposition);
  if (encoded) {
    try {
      return sanitize(decodeURIComponent(encoded[1].trim()));
    } catch {
      // malformed encoding: fall back to the plain form
    }
  }
  const quoted = /filename\s*=\s*"([^"]*)"/i.exec(contentDisposition);
  if (quoted) {
    return sanitize(quoted[1]);
  }
  const bare = /filename\s*=\s*([^;]+)/i.exec(contentDisposition);
  return bare ? sanitize(bare[1].trim()) : null;
}

/** Never let a header choose a path: keep only the last segment. */
function sanitize(name: string): string | null {
  const last = name.split(/[\\/]/).pop()?.trim() ?? '';
  return last.length > 0 ? last : null;
}
