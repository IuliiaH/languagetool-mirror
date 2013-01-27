/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import junit.framework.Assert;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.WordData;

import org.languagetool.tagging.BaseTagger;
import org.languagetool.tagging.Tagger;
import org.languagetool.tagging.disambiguation.Disambiguator;
import org.languagetool.tokenizers.SentenceTokenizer;
import org.languagetool.tokenizers.Tokenizer;
import org.languagetool.tools.StringTools;

/**
 * @author Daniel Naber
 */
public final class TestTools {

  private TestTools() {
  }

  public static ResourceBundle getEnglishMessages() {
    return getMessages("en");
  }

  public static Set<Language> getLanguagesExcept(String[] langCodes) {
    final Set<Language> languages = new HashSet<Language>();
    languages.addAll(Arrays.asList(Language.LANGUAGES));
    if (langCodes != null) {
      for (String langCode : langCodes) {
        final Language lang = Language.getLanguageForShortName(langCode);
        languages.remove(lang);
      }
    }
    return languages;
  }

  /**
   * Gets the resource bundle for the specified language.
   * @param language lowercase two-letter ISO-639 code.
   * @return the resource bundle for the specified language.
   */
  public static ResourceBundle getMessages(String language) {
    final ResourceBundle messages = ResourceBundle.getBundle(
        "org.languagetool.MessagesBundle", new Locale(language));
    return messages;
  }

  public static void testSplit(final String[] sentences,
      final SentenceTokenizer sTokenizer) {
    final StringBuilder inputString = new StringBuilder();
    final List<String> input = new ArrayList<String>();
    Collections.addAll(input, sentences);
    for (final String string : input) {
      inputString.append(string);
    }
    Assert.assertEquals(input, sTokenizer.tokenize(inputString.toString()));
  }

  public static void myAssert(final String input, final String expected,
      final Tokenizer tokenizer, final Tagger tagger) throws IOException {
    final List<String> tokens = tokenizer.tokenize(input);
    final List<String> noWhitespaceTokens = new ArrayList<String>();
    // whitespace confuses tagger, so give it the tokens but no whitespace
    // tokens:
    for (final String token : tokens) {
      if (isWord(token)) {
        noWhitespaceTokens.add(token);
      }
    }
    final List<AnalyzedTokenReadings> output = tagger.tag(noWhitespaceTokens);
    final StringBuilder outputStr = new StringBuilder();
    for (final Iterator<AnalyzedTokenReadings> iter = output.iterator(); iter
        .hasNext();) {
      final AnalyzedTokenReadings token = iter.next();
      final int readingsNumber = token.getReadingsLength();
      final List<String> readings = new ArrayList<String>();
      for (int j = 0; j < readingsNumber; j++) {
        final StringBuilder readingStr = new StringBuilder();
        readingStr.append(token.getAnalyzedToken(j).getToken());
        readingStr.append("/[");
        readingStr.append(token.getAnalyzedToken(j).getLemma());
        readingStr.append(']');
        readingStr.append(token.getAnalyzedToken(j).getPOSTag());
        readings.add(readingStr.toString());
      }
      // force some order on the result just for the test case - order may vary
      // from one version of the lexicon to the next:
      Collections.sort(readings);
      outputStr.append(StringTools.listToString(readings, "|"));
      if (iter.hasNext()) {
        outputStr.append(" -- ");
      }
    }
    Assert.assertEquals(expected, outputStr.toString());
  }

  public static void myAssert(final String input, final String expected,
      final Tokenizer tokenizer, final SentenceTokenizer sentenceTokenizer,
      final Tagger tagger, final Disambiguator disambiguator)
      throws IOException {
    final StringBuilder outputStr = new StringBuilder();
    final List<String> sentences = sentenceTokenizer.tokenize(input);
    for (final String sentence : sentences) {
      final List<String> tokens = tokenizer.tokenize(sentence);
      final List<String> noWhitespaceTokens = new ArrayList<String>();
      // whitespace confuses tagger, so give it the tokens but no whitespace
      // tokens:
      for (final String token : tokens) {
        if (isWord(token)) {
          noWhitespaceTokens.add(token);
        }
      }
      final List<AnalyzedTokenReadings> aTokens = tagger
          .tag(noWhitespaceTokens);
      final AnalyzedTokenReadings[] tokenArray = new AnalyzedTokenReadings[tokens
          .size() + 1];
      final AnalyzedToken[] startTokenArray = new AnalyzedToken[1];
      int toArrayCount = 0;
      final AnalyzedToken sentenceStartToken = new AnalyzedToken("",
          "SENT_START", null);
      startTokenArray[0] = sentenceStartToken;
      tokenArray[toArrayCount++] = new AnalyzedTokenReadings(startTokenArray, 0);
      int startPos = 0;
      int noWhitespaceCount = 0;
      for (final String tokenStr : tokens) {
        AnalyzedTokenReadings posTag = null;
        if (isWord(tokenStr)) {
          posTag = aTokens.get(noWhitespaceCount);
          posTag.setStartPos(startPos);
          noWhitespaceCount++;
        } else {
          posTag = tagger.createNullToken(tokenStr, startPos);
        }
        tokenArray[toArrayCount++] = posTag;
        startPos += tokenStr.length();
      }

      AnalyzedSentence finalSentence = new AnalyzedSentence(tokenArray);
      // disambiguate assigned tags
      finalSentence = disambiguator.disambiguate(finalSentence);

      final AnalyzedTokenReadings[] output = finalSentence.getTokens();

      for (int i = 0; i < output.length; i++) {
        final AnalyzedTokenReadings token = output[i];
        final int readingsNumber = token.getReadingsLength();
        final List<String> readings = new ArrayList<String>();
        for (int j = 0; j < readingsNumber; j++) {
          final StringBuilder readingStr = new StringBuilder();
          readingStr.append(token.getAnalyzedToken(j).getToken());
          readingStr.append("/[");
          readingStr.append(token.getAnalyzedToken(j).getLemma());
          readingStr.append(']');
          readingStr.append(token.getAnalyzedToken(j).getPOSTag());
          readings.add(readingStr.toString());
        }
        // force some order on the result just for the test case - order may vary
        // from one version of the lexicon to the next:
        Collections.sort(readings);
        outputStr.append(StringTools.listToString(readings, "|"));
        if (i < output.length - 1) {
          outputStr.append(' ');
        }
      }
    }
    Assert.assertEquals(expected, outputStr.toString());
  }

  public static boolean isWord(final String token) {
    for (int i = 0; i < token.length(); i++) {
      final char c = token.charAt(i);
      if (Character.isLetter(c) || Character.isDigit(c)) {
        return true;
      }
    }
    return false;
  }

  public static String callStringStaticMethod(final Class targetClass,
      final String methodName, final Class[] argClasses,
      final Object[] argObjects) throws InvocationTargetException,
      IllegalArgumentException, IllegalAccessException, SecurityException,
      NoSuchMethodException {
    final Method method = targetClass.getDeclaredMethod(methodName, argClasses);
    method.setAccessible(true);
    return (String) method.invoke(null, argObjects);
  }

  public static void testDictionary(BaseTagger tagger, Language language) throws IOException {
    final Dictionary dictionary = Dictionary.read(JLanguageTool.getDataBroker().getFromResourceDirAsUrl(tagger.getFileName()));
    final DictionaryLookup lookup = new DictionaryLookup(dictionary);
    for (WordData wordData : lookup) {
      if (wordData.getTag() == null || wordData.getTag().length() == 0) {
        System.err.println("**** Warning: " + language + ": the word " + wordData.getWord() + "/" + wordData.getStem() + " lacks a POS tag in the dictionary.");
      }
    }
  }

}
