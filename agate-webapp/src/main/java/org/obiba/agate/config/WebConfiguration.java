package org.obiba.agate.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.obiba.agate.web.filter.CachingHttpHeadersFilter;
import org.obiba.agate.web.filter.StaticResourcesProductionFilter;
import org.obiba.shiro.web.filter.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.MetricsServlet;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;
import static org.obiba.agate.web.rest.config.JerseyConfiguration.WS_ROOT;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
@ComponentScan({ "org.obiba.agate", "org.obiba.shiro" })
@PropertySource("classpath:agate-webapp.properties")
@AutoConfigureAfter(SecurityConfiguration.class)
public class WebConfiguration implements ServletContextInitializer, JettyServerCustomizer, EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(WebConfiguration.class);

  private static final int DEFAULT_HTTPS_PORT = 8444;

  private static final int MAX_IDLE_TIME = 30000;

  private static final int REQUEST_HEADER_SIZE = 8192;

  @Inject
  private Environment env;

  @Inject
  private MetricRegistry metricRegistry;

  @Inject
  private AuthenticationFilter authenticationFilter;

  @Inject
  private org.obiba.ssl.SslContextFactory sslContextFactory;

  private int httpsPort;

  @Override
  public void setEnvironment(Environment environment) {
    RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "https.");
    httpsPort = propertyResolver.getProperty("port", Integer.class, DEFAULT_HTTPS_PORT);
  }

  @Bean
  EmbeddedServletContainerCustomizer containerCustomizer() throws Exception {
    return (ConfigurableEmbeddedServletContainer container) -> {
      JettyEmbeddedServletContainerFactory jetty = (JettyEmbeddedServletContainerFactory) container;
      jetty.setServerCustomizers(Collections.singleton(this));
    };
  }

  @Override
  public void customize(Server server) {
    customizeSsl(server);
  }

  private void customizeSsl(Server server) {
    SslContextFactory jettySsl = new SslContextFactory() {

      @Override
      protected void doStart() throws Exception {
        setSslContext(sslContextFactory.createSslContext());
      }

      @Override
      public void checkKeyStore() {
      }
    };
    jettySsl.setWantClientAuth(true);
    jettySsl.setNeedClientAuth(false);
    jettySsl.addExcludeProtocols("SSLv2", "SSLv3");

    ServerConnector sslConnector = new ServerConnector(server, jettySsl);
    sslConnector.setPort(httpsPort);
    sslConnector.setIdleTimeout(MAX_IDLE_TIME);

    server.addConnector(sslConnector);
  }

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    log.info("Web application configuration, using profiles: {}", Arrays.toString(env.getActiveProfiles()));

    initAllowedMethodsFilter(servletContext);
    initAuthenticationFilter(servletContext);

    EnumSet<DispatcherType> disps = EnumSet.of(REQUEST, FORWARD, ASYNC);
    initMetrics(servletContext, disps);
    if(env.acceptsProfiles(Constants.SPRING_PROFILE_PRODUCTION)) {
      initStaticResourcesProductionFilter(servletContext, disps);
      initCachingHttpHeadersFilter(servletContext, disps);
    }
    initGzipFilter(servletContext, disps);

    log.info("Web application fully configured");
  }

  private void initAllowedMethodsFilter(ServletContext servletContext) {
    log.debug("Registering Allowed Methods Filter");

    FilterRegistration.Dynamic filterRegistration = servletContext.addFilter("noTrace", new NoTraceFilter());

    filterRegistration.addMappingForUrlPatterns(EnumSet.of(REQUEST, FORWARD, ASYNC, INCLUDE, ERROR), true, "/*");
    filterRegistration.setAsyncSupported(true);
  }

  private void initAuthenticationFilter(ServletContext servletContext) {
    log.debug("Registering Authentication Filter");
    FilterRegistration.Dynamic filterRegistration = servletContext
      .addFilter("authenticationFilter", authenticationFilter);

    if (filterRegistration == null) {
      filterRegistration =
        (FilterRegistration.Dynamic)servletContext.getFilterRegistration("authenticationFilter");
    }

    log.debug("Adding mapping to authentication filter registration");

    filterRegistration.addMappingForUrlPatterns(EnumSet.of(REQUEST, FORWARD, ASYNC, INCLUDE, ERROR), true,
      WS_ROOT + "/*");
    filterRegistration.setAsyncSupported(true);
  }

  /**
   * Initializes the GZip filter.
   */
  private void initGzipFilter(ServletContext servletContext, EnumSet<DispatcherType> disps) {
    log.debug("Registering GZip Filter");

    FilterRegistration.Dynamic compressingFilter = servletContext.addFilter("gzipFilter", new GzipFilter());

    if (compressingFilter == null) {
      compressingFilter = (FilterRegistration.Dynamic)servletContext.getFilterRegistration("gzipFilter");
    }

    compressingFilter.addMappingForUrlPatterns(disps, true, "*.css");
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.json");
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.html");
    compressingFilter.addMappingForUrlPatterns(disps, true, "*.js");
    compressingFilter.addMappingForUrlPatterns(disps, true, "/metrics/*");
    compressingFilter.addMappingForUrlPatterns(disps, true, WS_ROOT + "/*");
    compressingFilter.setAsyncSupported(true);
  }

  /**
   * Initializes the static resources production Filter.
   */
  private void initStaticResourcesProductionFilter(ServletContext servletContext, EnumSet<DispatcherType> disps) {

    log.debug("Registering static resources production Filter");
    FilterRegistration.Dynamic resourcesFilter = servletContext
        .addFilter("staticResourcesProductionFilter", new StaticResourcesProductionFilter());

    resourcesFilter.addMappingForUrlPatterns(disps, true, "/favicon.ico");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/robots.txt");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/index.html");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/images/*");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/fonts/*");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/scripts/*");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/styles/*");
    resourcesFilter.addMappingForUrlPatterns(disps, true, "/views/*");
    resourcesFilter.setAsyncSupported(true);
  }

  /**
   * Initializes the caching HTTP Headers Filter.
   */
  private void initCachingHttpHeadersFilter(ServletContext servletContext, EnumSet<DispatcherType> disps) {
    log.debug("Registering Caching HTTP Headers Filter");
    FilterRegistration.Dynamic cachingFilter = servletContext
        .addFilter("cachingHttpHeadersFilter", new CachingHttpHeadersFilter());

    cachingFilter.addMappingForUrlPatterns(disps, true, "/images/*");
    cachingFilter.addMappingForUrlPatterns(disps, true, "/fonts/*");
    cachingFilter.addMappingForUrlPatterns(disps, true, "/scripts/*");
    cachingFilter.addMappingForUrlPatterns(disps, true, "/styles/*");
    cachingFilter.setAsyncSupported(true);
  }

  /**
   * Initializes Metrics.
   */
  private void initMetrics(ServletContext servletContext, EnumSet<DispatcherType> disps) {
    log.debug("Initializing Metrics registries");
    servletContext.setAttribute(InstrumentedFilter.REGISTRY_ATTRIBUTE, metricRegistry);
    servletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);

    log.debug("Registering Metrics Filter");
    FilterRegistration.Dynamic metricsFilter = servletContext
        .addFilter("webappMetricsFilter", new InstrumentedFilter());

    metricsFilter.addMappingForUrlPatterns(disps, true, "/*");
    metricsFilter.setAsyncSupported(true);

    log.debug("Registering Metrics Servlet");
    ServletRegistration.Dynamic metricsAdminServlet = servletContext.addServlet("metricsServlet", new MetricsServlet());

    metricsAdminServlet.addMapping("/metrics/metrics/*");
    metricsAdminServlet.setAsyncSupported(true);
    metricsAdminServlet.setLoadOnStartup(2);
  }

  /**
   * When a TRACE request is received, returns a Forbidden response.
   */
  private static class NoTraceFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      if("TRACE".equals(httpRequest.getMethod())) {
        httpResponse.reset();
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "TRACE method not allowed");
        return;
      }
      chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
  }

}
