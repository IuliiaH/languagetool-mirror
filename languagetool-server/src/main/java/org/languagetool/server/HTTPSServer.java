/* LanguageTool, a natural language style checker
 * Copyright (C) 2012 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.server;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.languagetool.JLanguageTool;
import org.languagetool.gui.Tools;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.ResourceBundle;
import java.util.Set;

import static org.languagetool.server.HTTPServerConfig.DEFAULT_HOST;

/**
 * A small embedded HTTPS server that checks text. Works <strong>only</strong> with https, not with http.
 *
 * @see HTTPServer
 * @since 2.0
 */
public class HTTPSServer extends Server {

  /**
   * Prepare a server on the given host and port - use run() to start it.
   * @param runInternally if true, then the server was started from the GUI.
   * @param host the host to bind to, e.g. <code>"localhost"</code> or <code>null</code> to bind to any host
   * @param allowedIps the IP addresses from which connections are allowed or <code>null</code> to allow any host
   * @throws PortBindingException if we cannot bind to the given port, e.g. because something else is running there
   */
  public HTTPSServer(HTTPSServerConfig config, boolean runInternally, String host, Set<String> allowedIps) {
    this.port = config.getPort();
    this.host = host;
    try {
      if (host == null) {
        server = HttpsServer.create(new InetSocketAddress(port), 0);
      } else {
        server = HttpsServer.create(new InetSocketAddress(host, port), 0);
      }
      final SSLContext sslContext = getSslContext(config.getKeystore(), config.getKeyStorePassword());
      final HttpsConfigurator configurator = getConfigurator(sslContext);
      ((HttpsServer)server).setHttpsConfigurator(configurator);
      final RequestLimiter limiter = getRequestLimiterOrNull(config);
      final LanguageToolHttpHandler httpHandler = new LanguageToolHttpHandler(config.isVerbose(), allowedIps, runInternally, limiter);
      httpHandler.setMaxTextLength(config.getMaxTextLength());
      httpHandler.setAllowOriginUrl(config.getAllowOriginUrl());
      server.createContext("/", httpHandler);
    } catch (BindException e) {
      final ResourceBundle messages = JLanguageTool.getMessageBundle();
      final String message = Tools.makeTexti18n(messages, "https_server_start_failed", host, Integer.toString(port));
      throw new PortBindingException(message, e);
    } catch (Exception e) {
      final ResourceBundle messages = JLanguageTool.getMessageBundle();
      final String message = Tools.makeTexti18n(messages, "https_server_start_failed_unknown_reason", host, Integer.toString(port));
      throw new RuntimeException(message, e);
    }
  }

  private RequestLimiter getRequestLimiterOrNull(HTTPSServerConfig config) {
    final int requestLimit = config.getRequestLimit();
    final int requestLimitPeriodInSeconds = config.getRequestLimitPeriodInSeconds();
    if (requestLimit > 0 || requestLimitPeriodInSeconds > 0) {
      return new RequestLimiter(requestLimit, requestLimitPeriodInSeconds);
    }
    return null;
  }

  private SSLContext getSslContext(File keyStoreFile, String passPhrase) throws IOException {
    final FileInputStream keyStoreStream = new FileInputStream(keyStoreFile);
    try {
      final KeyStore keystore = KeyStore.getInstance("JKS");
      keystore.load(keyStoreStream, passPhrase.toCharArray());
      final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(keystore, passPhrase.toCharArray());
      final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(keystore);
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
      return sslContext;
    } catch (Exception e) {
      throw new RuntimeException("Could not set up SSL context", e);
    } finally {
      keyStoreStream.close();
    }
  }

  private HttpsConfigurator getConfigurator(final SSLContext sslContext) {
    return new HttpsConfigurator(sslContext) {
          @Override
          public void configure (HttpsParameters params) {
            final SSLContext context = getSSLContext();
            final SSLParameters sslParams = context.getDefaultSSLParameters();
            params.setNeedClientAuth(false);
            params.setSSLParameters(sslParams);
          }
        };
  }

  public static void main(String[] args) throws IOException {
    if (args.length > 7 || usageRequested(args)) {
      System.out.println("Usage: " + HTTPSServer.class.getSimpleName()
              + " --config propertyFile [--port|-p port] [--public]");
      System.out.println("  --config file  a Java property file with values for:");
      System.out.println("                 'keystore' - a Java keystore with an SSL certificate");
      System.out.println("                 'password' - the keystore's password");
      System.out.println("                 'maxTextLength' - maximum text length, longer texts will cause an error (optional)");
      printCommonOptions();
      System.exit(1);
    }
    final boolean runInternal = false;
    final HTTPSServerConfig config = new HTTPSServerConfig(args);
    try {
      final HTTPSServer server;
      if (config.isPublicAccess()) {
        System.out.println("WARNING: running in public mode, LanguageTool API can be accessed without restrictions!");
        server = new HTTPSServer(config, runInternal, null, null);
      } else {
        server = new HTTPSServer(config, runInternal, DEFAULT_HOST, DEFAULT_ALLOWED_IPS);
      }
      server.run();
    } catch (Exception e) {
      throw new RuntimeException("Could not start LanguageTool HTTPS server on " + HTTPServerConfig.DEFAULT_HOST + ", port " + config.getPort(), e);
    }
  }

  @Override
  protected String getProtocol() {
    return "https";
  }

}
