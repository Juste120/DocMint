/**
 * Public API of DocMint.
 *
 * <p>The common case is one line:
 *
 * <pre>{@code
 * byte[] pdf = DocMint.createDefault().pdf("invoice", invoice);
 * }</pre>
 *
 * <p>{@link io.github.juste120.docmint.api.DocMint} is the entry point, configured with {@link
 * io.github.juste120.docmint.api.DocMintBuilder}. Every error is an unchecked {@link
 * io.github.juste120.docmint.api.DocMintException} carrying a stable {@linkplain
 * io.github.juste120.docmint.api.DocMintException#code() code}.
 */
package io.github.juste120.docmint.api;
