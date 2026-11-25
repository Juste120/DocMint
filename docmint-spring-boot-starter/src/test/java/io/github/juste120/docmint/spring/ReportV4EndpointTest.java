package io.github.juste120.docmint.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.juste120.docmint.spring.testapp.TestApplication;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** V4 in Spring: streamed CSV and CompletableFuture results. */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class ReportV4EndpointTest {

  @Autowired private MockMvc mvc;

  @Test
  void streamIsWrittenRowByRow() throws Exception {
    MvcResult result =
        mvc.perform(get("/v4/stream.csv"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        startsWith("inline; filename=\"flux.csv\"")))
            .andReturn();

    assertThat(new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
        .isEqualTo("﻿name;code\r\nClient 0;C-0\r\nClient 1;C-1\r\nClient 2;C-2\r\n");
    assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_LENGTH)).isNull();
  }

  @Test
  void completableFutureIsRenderedWhenComplete() throws Exception {
    MvcResult started =
        mvc.perform(get("/v4/async")).andExpect(request().asyncStarted()).andReturn();

    mvc.perform(asyncDispatch(started))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    startsWith("inline; filename=\"invoice.pdf\"")));
  }
}
