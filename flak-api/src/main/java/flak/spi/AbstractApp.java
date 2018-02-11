package flak.spi;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import flak.App;
import flak.ContentTypeProvider;
import flak.ErrorHandler;
import flak.InputParser;
import flak.OutputFormatter;
import flak.Request;
import flak.RequestHandler;
import flak.Response;
import flak.SessionManager;
import flak.SuccessHandler;
import flak.UnknownPageHandler;
import flak.WebServer;
import flak.annotations.Route;
import flak.util.Log;

public abstract class AbstractApp implements App {

  /**
   * Optional URL where the app is plugged.
   */
  protected final String rootUrl;

  protected final Map<String, RequestHandler> handlers = new Hashtable<>();

  protected ContentTypeProvider mime = new DefaultContentTypeProvider();

  private final Map<String, OutputFormatter<?>> outputFormatterMap =
    new Hashtable<>();

  private final Map<String, InputParser> inputParserMap = new Hashtable<>();

  protected SessionManager sessionManager;

  protected Vector<ErrorHandler> errorHandlers = new Vector<>();

  protected Vector<SuccessHandler> successHandlers = new Vector<>();

  protected UnknownPageHandler unknownPageHandler;

  public AbstractApp(String rootUrl) {
    this.rootUrl = rootUrl;
    sessionManager = new DefaultSessionManager(this);
  }

  public String getRootUrl() {
    WebServer srv = getServer();
    return srv.getProtocol() + "://" + srv.getHostName() + ":" + srv.getPort() + getPath();
  }

  public String getPath() {
    return rootUrl == null ? "" : rootUrl;
  }

  /**
   * Scans specified object for route handlers, i.e. public methods with @Route
   * annotation.
   *
   * @see Route
   */
  public App scan(Object obj) {
    for (Method method : obj.getClass().getMethods()) {
      Route route = method.getAnnotation(Route.class);
      if (route != null) {
        addHandler(route, method, obj);
      }
    }
    return this;
  }

  protected abstract void addHandler(Route route, Method method, Object obj);

  public App addOutputFormatter(String name, OutputFormatter<?> conv) {
    outputFormatterMap.put(name, conv);
    reconfigureHandlers();
    return this;
  }

  public OutputFormatter<?> getOutputFormatter(String name) {
    return outputFormatterMap.get(name);
  }

  @Override
  public App addInputParser(String name, InputParser inputParser) {
    inputParserMap.put(name, inputParser);
    reconfigureHandlers();
    return this;
  }

  public InputParser getInputParser(String name) {
    return inputParserMap.get(name);
  }

  protected String makeAbsoluteUrl(String uri) {
    if (rootUrl != null) {
      if (uri.startsWith("/"))
        uri = rootUrl + uri;
      else
        uri = rootUrl + "/" + uri;
    }
    return uri;
  }

  public void setContentTypeProvider(ContentTypeProvider mime) {
    this.mime = mime;
  }

  /**
   * Returns true if in DEBUG mode. When in debug mode, server stack traces are
   * sent to clients as body of the 500 response.
   */
  public boolean isDebugEnabled() {
    return Log.DEBUG;
  }

  public void setSessionManager(SessionManager mgr) {
    this.sessionManager = mgr;
  }

  /**
   * Replies to current request with an HTTP redirect response with specified
   * location.
   */
  public Response redirect(String location) {
    Response r = getResponse();
    r.addHeader("Location", location);
    r.setStatus(HttpURLConnection.HTTP_MOVED_TEMP);
    return r;
  }

  public void redirect(Response r, String location) {
    r.redirect(location);
  }

  protected abstract void reconfigureHandlers();

  /**
   * Adds a handler that will be notified whenever a request is rejected
   */
  public void addErrorHandler(ErrorHandler hook) {
    errorHandlers.add(hook);
  }

  /**
   * Adds a handler that will be notified whenever a request is successful
   */
  public void addSuccessHandler(SuccessHandler hook) {
    successHandlers.add(hook);
  }

  public void fireError(int status, Request req, Throwable t) {
    for (ErrorHandler errorHandler : errorHandlers) {
      try {
        errorHandler.onError(status, req, t);
      }
      catch (Exception e) {
        Log.error(e, e);
      }
    }
  }

  public void fireSuccess(Method method, Object[] args, Object res) {
    Request r = getRequest();
    for (SuccessHandler successHandler : successHandlers) {
      successHandler.onSuccess(r, method, args, res);
    }
  }

  /**
   * Experimental. Allows to handle a request for an URL with no handler.
   * Requires
   * a root handler to be set somewhere (i.e. Route("/").
   */
  public void setUnknownPageHandler(UnknownPageHandler unknownPageHandler) {
    this.unknownPageHandler = unknownPageHandler;
  }

  public String relativePath(String path) {
    return rootUrl == null || rootUrl.equals("/") ? path
                                                  : path.substring(rootUrl.length());
  }

  @Override
  public String absolutePath(String path) {
    return rootUrl == null || rootUrl.equals("/") ? path : rootUrl + path;
  }

  public SessionManager getSessionManager() {
    return sessionManager;
  }
}
