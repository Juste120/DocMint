/**
 * Ports (service provider interfaces) of DocMint's hexagonal architecture.
 *
 * <p>The application core only talks to these interfaces. Default implementations live in the
 * internal {@code infrastructure} package. Experts can plug their own implementation through {@link
 * io.github.juste120.docmint.api.DocMintBuilder#templateResolver(TemplateResolver)} and {@link
 * io.github.juste120.docmint.api.DocMintBuilder#renderer(DocumentRenderer)}.
 */
package io.github.juste120.docmint.port;
