/**
 * Ports (service provider interfaces) of Agbale's hexagonal architecture.
 *
 * <p>The application core only talks to these interfaces. Default implementations live in the
 * internal {@code infrastructure} package. Experts can plug their own implementation through
 * {@link io.github.juste120.agbale.api.AgbaleBuilder#templateResolver(TemplateResolver)} and
 * {@link io.github.juste120.agbale.api.AgbaleBuilder#renderer(DocumentRenderer)}.
 */
package io.github.juste120.agbale.port;
