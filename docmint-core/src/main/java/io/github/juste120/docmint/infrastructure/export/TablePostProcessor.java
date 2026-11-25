package io.github.juste120.docmint.infrastructure.export;

import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.port.PostProcessContext;
import io.github.juste120.docmint.port.PostProcessor;
import io.github.juste120.docmint.port.TableData;
import io.github.juste120.docmint.port.TableWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Converts the HTML of a template into a tabular format with a {@link TableWriter}. */
public final class TablePostProcessor implements PostProcessor {

  private final TableWriter writer;
  private final TableExtractor extractor = new TableExtractor();

  /**
   * Creates the post-processor.
   *
   * @param writer the writer of the target format
   */
  public TablePostProcessor(TableWriter writer) {
    this.writer = writer;
  }

  @Override
  public boolean appliesTo(RenderedDocument document, PostProcessContext context) {
    return PostProcessor.is(document, OutputFormat.HTML)
        && context.requestedFormat() == writer.format();
  }

  @Override
  public RenderedDocument process(RenderedDocument document, PostProcessContext context) {
    String template = context.templateName().value();
    List<TableData> tables =
        extractor.extract(new String(document.content(), StandardCharsets.UTF_8));
    if (tables.isEmpty()) {
      throw new RenderException(
          "Template '"
              + template
              + "' has no <table> to export as "
              + writer.format()
              + ". Add a table (with class docmint-export to choose which one).",
          null);
    }
    checkRows(tables, template, context.limits().maxExportRows());
    return new RenderedDocument(
        writer.write(tables, context.options()), writer.format(), tables.size());
  }

  /** Checks the export row limit. */
  public static void checkRows(List<TableData> tables, String name, int maxRows) {
    long rows = tables.stream().mapToLong(TableData::dataRowCount).sum();
    if (rows > maxRows) {
      throw new LimitExceededException(
          name, LimitExceededException.Limit.EXPORT_ROWS, maxRows + " rows");
    }
  }
}
