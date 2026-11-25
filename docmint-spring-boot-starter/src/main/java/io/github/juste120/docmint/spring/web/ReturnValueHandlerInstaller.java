package io.github.juste120.docmint.spring.web;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Puts {@link ReportReturnValueHandler} first in Spring MVC's return value handlers.
 *
 * <p>Handlers registered through {@code WebMvcConfigurer} run after the built-in ones, so on a
 * {@code @RestController} the {@code @ResponseBody} handler would serialise the DTO to JSON before
 * DocMint could see it. Prepending is the documented way to take precedence.
 */
final class ReturnValueHandlerInstaller implements SmartInitializingSingleton {

  private final RequestMappingHandlerAdapter adapter;
  private final ReportReturnValueHandler handler;

  ReturnValueHandlerInstaller(
      RequestMappingHandlerAdapter adapter, ReportReturnValueHandler handler) {
    this.adapter = adapter;
    this.handler = handler;
  }

  @Override
  public void afterSingletonsInstantiated() {
    List<HandlerMethodReturnValueHandler> current = adapter.getReturnValueHandlers();
    List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
    handlers.add(handler);
    if (current != null) {
      current.stream().filter(h -> h != handler).forEach(handlers::add);
    }
    adapter.setReturnValueHandlers(handlers);
  }
}
