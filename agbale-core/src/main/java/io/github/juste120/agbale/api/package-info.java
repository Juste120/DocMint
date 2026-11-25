/**
 * Public API of Agbale.
 *
 * <p>The common case is one line:
 *
 * <pre>{@code
 * byte[] pdf = Agbale.createDefault().pdf("invoice", invoice);
 * }</pre>
 *
 * <p>{@link io.github.juste120.agbale.api.Agbale} is the entry point, configured with {@link
 * io.github.juste120.agbale.api.AgbaleBuilder}. Every error is an unchecked {@link
 * io.github.juste120.agbale.api.AgbaleException} carrying a stable {@linkplain
 * io.github.juste120.agbale.api.AgbaleException#code() code}.
 */
package io.github.juste120.agbale.api;
