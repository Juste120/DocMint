package io.github.juste120.docmint.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.juste120.docmint.spring.testapp.TestApplication;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** V3 in Spring: CSV and HTML formats, list exports without template. */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class ReportV3EndpointTest {

  @Autowired private MockMvc mvc;

  private String body(String url) throws Exception {
    return new String(
        mvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray(),
        StandardCharsets.UTF_8);
  }

  @Test
  void templateTableAsCsv() throws Exception {
    mvc.perform(get("/v3/table.csv"))
        .andExpect(content().contentTypeCompatibleWith("text/csv"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION, startsWith("inline; filename=\"table.csv\"")));
    assertThat(body("/v3/table.csv")).isEqualTo("﻿Mois;Ventes\r\nJanvier;1250000\r\n");
  }

  @Test
  void listExportsWithoutTemplate() throws Exception {
    mvc.perform(get("/v3/list.csv"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    startsWith("inline; filename=\"clients.csv\"")));
    assertThat(body("/v3/list.csv")).isEqualTo("﻿name;code\r\nKossi Mensah;C-01\r\n'=1+1;C-02\r\n");
    mvc.perform(get("/v3/list-default.csv"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    startsWith("inline; filename=\"export.csv\"")));
    assertThat(body("/v3/list-default.csv")).isEqualTo("name,code\r\nAma,C-03\r\n");
  }

  @Test
  void htmlFormatAndErrors() throws Exception {
    assertThat(body("/v3/table.html")).contains("<th>Mois</th>");
    mvc.perform(get("/v3/no-template"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("DOCMINT_INVALID_REPORT_METHOD"));
  }
}
