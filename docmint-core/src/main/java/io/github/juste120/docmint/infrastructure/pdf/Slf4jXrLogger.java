package io.github.juste120.docmint.infrastructure.pdf;

import com.openhtmltopdf.util.Diagnostic;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRLogger;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends OpenHTMLtoPDF's internal logs to SLF4J at DEBUG (warnings) and TRACE (information) level,
 * under {@code io.github.juste120.docmint.pdf.engine}. Without it the engine prints INFO lines on
 * the console for every document. Problems that matter (refused or missing resources) are reported
 * by DocMint itself.
 */
final class Slf4jXrLogger implements XRLogger {

  private static final Logger LOG =
      LoggerFactory.getLogger("io.github.juste120.docmint.pdf.engine");

  private Slf4jXrLogger() {}

  /** Installs this logger unless the application already configured its own. */
  static synchronized void installIfDefault() {
    XRLogger current = XRLog.getLoggerImpl();
    if (current == null || current.getClass().getName().endsWith("JDKXRLogger")) {
      XRLog.setLoggerImpl(new Slf4jXrLogger());
    }
  }

  @Override
  public void log(String where, Level level, String msg) {
    log(where, level, msg, null);
  }

  @Override
  public void log(String where, Level level, String msg, Throwable th) {
    if (level.intValue() >= Level.WARNING.intValue()) {
      LOG.debug("[{}] {}", where, msg, th);
    } else {
      LOG.trace("[{}] {}", where, msg, th);
    }
  }

  @Override
  public void setLevel(String logger, Level level) {
    // Levels are configured in the application's SLF4J backend.
  }

  @Override
  public boolean isLogLevelEnabled(Diagnostic diagnostic) {
    // Warnings are always produced: DocMint's diagnostic consumer needs them.
    return diagnostic.getLevel().intValue() >= Level.WARNING.intValue() || LOG.isTraceEnabled();
  }
}
