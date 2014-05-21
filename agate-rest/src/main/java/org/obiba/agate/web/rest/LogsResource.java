package org.obiba.agate.web.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.obiba.agate.web.rest.dto.LoggerDTO;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.codahale.metrics.annotation.Timed;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Controller for view and managing Log Level at runtime.
 */
@Path("/logs")
public class LogsResource {

  @GET
  @Produces(APPLICATION_JSON)
  @Timed
  public List<LoggerDTO> getList() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    return context.getLoggerList().stream().map(LoggerDTO::new).collect(Collectors.toList());
  }

  @PUT
  @Timed
  public HttpStatus changeLevel(LoggerDTO jsonLogger) {
    @SuppressWarnings("TypeMayBeWeakened")
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.getLogger(jsonLogger.getName()).setLevel(Level.valueOf(jsonLogger.getLevel()));
    return HttpStatus.NO_CONTENT;
  }
}
