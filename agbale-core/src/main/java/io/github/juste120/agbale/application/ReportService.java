package io.github.juste120.agbale.application;

import io.github.juste120.agbale.api.RenderRequest;
import io.github.juste120.agbale.api.RenderResponse;
import java.util.List;

/** Use cases of Agbale: render a document, list templates, clear the cache. */
public interface ReportService {

  /**
   * Renders a document.
   *
   * @param request the request
   * @return the rendered document
   */
  RenderResponse render(RenderRequest request);

  /**
   * Lists the available templates.
   *
   * @return the template names, sorted
   */
  List<String> availableTemplates();

  /** Clears the template cache. */
  void clearCache();
}
