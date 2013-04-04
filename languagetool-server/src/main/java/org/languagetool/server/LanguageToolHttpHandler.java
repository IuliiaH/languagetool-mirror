/* LanguageTool, a natural language style checker
 * Copyright (C) 2011 Daniel Naber (http://www.danielnaber.de)
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.tika.language.LanguageIdentifier;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.gui.Configuration;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.bitext.BitextRule;
import org.languagetool.tools.StringTools;
import org.languagetool.tools.Tools;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

class LanguageToolHttpHandler implements HttpHandler {

  private static final String CONTENT_TYPE_VALUE = "text/xml; charset=UTF-8";
  private static final String ENCODING = "utf-8";
  private static final int CONTEXT_SIZE = 40; // characters
  private static final int MIN_LENGTH_FOR_AUTO_DETECTION = 60;  // characters

  private final Set<String> allowedIps;  
  private final boolean verbose;
  private final boolean internalServer;
  private final RequestLimiter requestLimiter;
  
  private Configuration config;
  
  private boolean useQuerySettings;
  
  private String[] enabledRules = {};
  private String[] disabledRules = {};
  private int maxTextLength = Integer.MAX_VALUE;

  /**
   * @param verbose print the input text in case of exceptions
   * @param allowedIps set of IPs that may connect or <tt>null</tt> to allow any IP
   * @param requestLimiter may be null
   */
  LanguageToolHttpHandler(boolean verbose, Set<String> allowedIps, boolean internal, RequestLimiter requestLimiter) throws IOException {
    this.verbose = verbose;
    this.allowedIps = allowedIps;
    this.internalServer = internal;
    this.requestLimiter = requestLimiter;
    config = new Configuration(null);
  }

  void setMaxTextLength(int maxTextLength) {
    this.maxTextLength = maxTextLength;
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    String text = null;
    try {
      final URI requestedUri = httpExchange.getRequestURI();
      final String remoteAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();
      if (requestLimiter != null && !requestLimiter.isAccessOkay(remoteAddress)) {
        final String errorMessage = "Error: Access from " + StringTools.escapeXML(remoteAddress) +
                " denied - too many requests. Allowed maximum requests: " + requestLimiter.getRequestLimit() +
                " requests per " + requestLimiter.getRequestLimitPeriodInSeconds() + " seconds";
        sendError(httpExchange, HttpURLConnection.HTTP_FORBIDDEN, errorMessage);
        throw new RuntimeException(errorMessage);
      }
      final Map<String, String> parameters = getRequestQuery(httpExchange, requestedUri);
      if (allowedIps == null || allowedIps.contains(remoteAddress)) {
        if (requestedUri.getRawPath().endsWith("/Languages")) {
          // request type: list known languages
          printListOfLanguages(httpExchange);
        } else {
          // request type: text checking
          text = parameters.get("text");
          if (text == null) {
            throw new IllegalArgumentException("Missing 'text' parameter");
          }
          checkText(text, httpExchange, parameters);
        }
      } else {
        final String errorMessage = "Error: Access from " + StringTools.escapeXML(remoteAddress) + " denied";
        sendError(httpExchange, HttpURLConnection.HTTP_FORBIDDEN, errorMessage);
        throw new RuntimeException(errorMessage);
      }
    } catch (Exception e) {
      if (verbose) {
        print("Exception was caused by this text: " + text);
      }
      e.printStackTrace();
      final String response = "Error: " + StringTools.escapeXML(Tools.getFullStackTrace(e));
      sendError(httpExchange, HttpURLConnection.HTTP_INTERNAL_ERROR, response);
    } finally {
      httpExchange.close();
    }
  }

  private void sendError(HttpExchange httpExchange, int returnCode, String response) throws IOException {
    httpExchange.sendResponseHeaders(returnCode, response.getBytes(ENCODING).length);
    httpExchange.getResponseBody().write(response.getBytes(ENCODING));
  }

  private Map<String, String> getRequestQuery(HttpExchange httpExchange, URI requestedUri) throws IOException {
    final String query;
    if ("post".equalsIgnoreCase(httpExchange.getRequestMethod())) {
      query = StringTools.streamToString(httpExchange.getRequestBody(), ENCODING);
    } else {
      query = requestedUri.getRawQuery();
    }
    return parseQuery(query);
  }

  private void printListOfLanguages(HttpExchange httpExchange) throws IOException {
    httpExchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_VALUE);
    final String response = getSupportedLanguagesAsXML();
    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.getBytes(ENCODING).length);
    httpExchange.getResponseBody().write(response.getBytes(ENCODING));
  }
  
  private static Language detectLanguageOfString(final String text, final String fallbackLanguage) {
    // TODO: use identifier.isReasonablyCertain() - but make sure it works!
    if (text.length() < MIN_LENGTH_FOR_AUTO_DETECTION && fallbackLanguage != null) {
      print("Auto-detected language of text with length " + text.length() + " is not reasonably certain, using '" + fallbackLanguage + "' as fallback");
      return Language.getLanguageForShortName(fallbackLanguage);
    }
    
    final LanguageIdentifier identifier = new LanguageIdentifier(text);
    Language lang;
    try {
      lang = Language.getLanguageForShortName(identifier.getLanguage());
    } catch (IllegalArgumentException e) {
      // fall back to English
      lang = Language.getLanguageForLocale(Locale.ENGLISH);
    }
    if (lang.getDefaultVariant() != null) {
      lang = lang.getDefaultVariant();
    }
    return lang;
  }

  private void checkText(String text, HttpExchange httpExchange, Map<String, String> parameters) throws Exception {
    final long timeStart = System.currentTimeMillis();
    if (text.length() > maxTextLength) {
      throw new IllegalArgumentException("Text is " + text.length() + " characters long, exceeding maximum length of " + maxTextLength);
    }
    final String langParam = parameters.get("language");
    final String autodetectParam = parameters.get("autodetect");
    if (langParam == null && (autodetectParam == null || !autodetectParam.equals("1"))) {
      throw new IllegalArgumentException("Missing 'language' parameter. Specify language or use autodetect=1 for auto-detecting the language of the input text.");
    }
    
    final Language lang;
    if (autodetectParam != null && autodetectParam.equals("1")) {
      lang = detectLanguageOfString(text, langParam);
      print("Auto-detected language: " + lang.getShortNameWithVariant());
    } else {
      lang = Language.getLanguageForShortName(langParam);
    }
    
    final String motherTongueParam = parameters.get("motherTongue");
    Language motherTongue = null;
    if (null != motherTongueParam) {
      motherTongue = Language.getLanguageForShortName(motherTongueParam);
    }

    final String enabledParam = parameters.get("enabled");
    enabledRules = new String[0];
    if (null != enabledParam) {
      enabledRules = enabledParam.split(",");
    }
    
    final String disabledParam = parameters.get("disabled");
    disabledRules = new String[0];
    if (null != disabledParam) {
      disabledRules = disabledParam.split(",");
    }
    
    useQuerySettings = enabledRules.length > 0 || disabledRules.length > 0; 
    
    final List<RuleMatch> matches;
    final String sourceText = parameters.get("srctext");
    if (sourceText == null) {
      final JLanguageTool lt = getLanguageToolInstance(lang, motherTongue);
      matches = lt.check(text);
    } else {
      if (motherTongueParam == null) {
        throw new IllegalArgumentException("Missing 'motherTongue' for bilingual checks");
      }
      print("Checking bilingual text, with source length " + sourceText.length() +
          " and target length " + text.length() + " (characters), source language " +
          motherTongue + " and target language " + langParam);
      final JLanguageTool sourceLt = getLanguageToolInstance(motherTongue, null);
      final JLanguageTool targetLt = getLanguageToolInstance(lang, null);
      final List<BitextRule> bRules = Tools.getBitextRules(motherTongue, lang);
      matches = Tools.checkBitext(sourceText, text, sourceLt, targetLt, bRules);
    }
    httpExchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_VALUE);
    final String response = StringTools.ruleMatchesToXML(matches, text,
            CONTEXT_SIZE, StringTools.XmlPrintMode.NORMAL_XML, lang, motherTongue);
    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.getBytes(ENCODING).length);
    httpExchange.getResponseBody().write(response.getBytes(ENCODING));
    String languageMessage = lang.getShortNameWithVariant();
    if (motherTongue != null) {
      languageMessage += " (mother tongue: " + motherTongue.getShortNameWithVariant() + ")";
    }
    print("Check done: " + text.length() + " characters, language " + languageMessage + ", "
            + (System.currentTimeMillis() - timeStart) + "ms");
  }

  private Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
    final Map<String, String> parameters = new HashMap<String, String>();
    if (query != null) {
      final String[] pairs = query.split("[&]");
      final Map<String, String> parameterMap = getParameterMap(pairs);
      parameters.putAll(parameterMap);
    }
    return parameters;
  }

  private Map<String, String> getParameterMap(String[] pairs) throws UnsupportedEncodingException {
    final Map<String, String> parameters = new HashMap<String, String>();
    for (String pair : pairs) {
      final int delimPos = pair.indexOf("=");
      if (delimPos != -1) {
        final String param = pair.substring(0, delimPos);
        final String key = URLDecoder.decode(param, ENCODING);
        final String value = URLDecoder.decode(pair.substring(delimPos + 1), ENCODING);
        parameters.put(key, value);
      }
    }
    return parameters;
  }

  private static void print(String s) {
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final String now = dateFormat.format(new Date());
    System.out.println(now + " " + s);
  }

  /**
   * Find or create a JLanguageTool instance for a specific language and mother tongue.
   * The instance will be reused. If any customization is required (like disabled rules), 
   * it will be done after acquiring this instance.
   * 
   * @param lang the language to be used.
   * @param motherTongue the user's mother tongue or <code>null</code>
   * @return a JLanguageTool instance for a specific language and mother tongue.
   * @throws Exception when JLanguageTool creation failed
   */
  private JLanguageTool getLanguageToolInstance(Language lang, Language motherTongue) throws Exception {
    final JLanguageTool newLanguageTool = new JLanguageTool(lang, motherTongue);
    newLanguageTool.activateDefaultPatternRules();
    newLanguageTool.activateDefaultFalseFriendRules();
    config = new Configuration(lang);
    if (!useQuerySettings && internalServer && config.getUseGUIConfig()) { // use the GUI config values
      configureGUI(newLanguageTool);
    }
    if (useQuerySettings) {
      Tools.selectRules(newLanguageTool, disabledRules, enabledRules, true);
    }
    return newLanguageTool;
  }

  private void configureGUI(JLanguageTool langTool) {
    print("Using options configured in the GUI");
    //TODO: add a parameter to config to set language
    final Set<String> disabledRules = config.getDisabledRuleIds();
    if (disabledRules != null) {
      for (final String ruleId : disabledRules) {
        langTool.disableRule(ruleId);
      }
    }
    final Set<String> disabledCategories = config.
            getDisabledCategoryNames();
    if (disabledCategories != null) {
      for (final String categoryName : disabledCategories) {
        langTool.disableCategory(categoryName);
      }
    }
    final Set<String> enabledRules = config.getEnabledRuleIds();
    if (enabledRules != null) {
      for (String ruleName : enabledRules) {
        langTool.enableDefaultOffRule(ruleName);
        langTool.enableRule(ruleName);
      }
    }
  }
 

  /**
   * Construct an XML string containing all supported languages. <br/>The XML format looks like this:<br/><br/>
   * &lt;languages&gt;<br/>
   *    &nbsp;&nbsp;&lt;language name="Catalan" abbr="ca" abbrWithVariant="ca-ES"/&gt;<br/>
   *    &nbsp;&nbsp;&lt;language name="German" abbr="de" abbrWithVariant="de"/&gt;<br/>
   *    &nbsp;&nbsp;&lt;language name="German (Germany)" abbr="de" abbrWithVariant="de-DE"/&gt;<br/>
   *  &lt;languages&gt;<br/><br/>
   *  The languages are sorted alphabetically by their name.
   * @return an XML document listing all supported languages
   */
  public static String getSupportedLanguagesAsXML() {
    final Language[] languageCopy = Language.REAL_LANGUAGES.clone();
    final List<Language> languages = Arrays.asList(languageCopy);
    Collections.sort(languages, new Comparator<Language>() {
      @Override
      public int compare(Language o1, Language o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    final StringBuilder xmlBuffer = new StringBuilder("<?xml version='1.0' encoding='" + ENCODING + "'?>\n<languages>\n");
    for (Language lang : languages) {
      xmlBuffer.append(String.format("\t<language name=\"%s\" abbr=\"%s\" abbrWithVariant=\"%s\"/> \n", lang.getName(),
              lang.getShortName(), lang.getShortNameWithVariant()));
    }
    xmlBuffer.append("</languages>\n");
    return xmlBuffer.toString();
  }
}
