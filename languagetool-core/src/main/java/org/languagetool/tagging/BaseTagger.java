/* LanguageTool, a natural language style checker 
 * Copyright (C) 2006 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.tagging;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.IStemmer;
import morfologik.stemming.WordData;

import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.tools.StringTools;

/**
 * Base tagger using Lametyzator.
 * 
 * @author Marcin Milkowski
 */
public abstract class BaseTagger implements Tagger {

  protected IStemmer dictLookup;
  protected Locale conversionLocale = Locale.getDefault();  
  boolean tagLowercaseWithUppercase = true;

  /**
   * Get the filename, e.g., <tt>/resource/fr/french.dict</tt>.
   */
  public abstract String getFileName();

  public void setLocale(Locale locale) {
    conversionLocale = locale;
  }

  @Override
  public List<AnalyzedTokenReadings> tag(final List<String> sentenceTokens)
  throws IOException {    
    List<AnalyzedToken> taggerTokens;
    List<AnalyzedToken> lowerTaggerTokens;
    List<AnalyzedToken> upperTaggerTokens;
    final List<AnalyzedTokenReadings> tokenReadings = new ArrayList<AnalyzedTokenReadings>();
    int pos = 0;
    // caching IStemmer instance - lazy init
    if (dictLookup == null) {
      final URL url = JLanguageTool.getDataBroker().getFromResourceDirAsUrl(getFileName());
      dictLookup = new DictionaryLookup(Dictionary.read(url));
    }

    for (String word : sentenceTokens) {
      final List<AnalyzedToken> l = new ArrayList<AnalyzedToken>();
      final String lowerWord = word.toLowerCase(conversionLocale);
      taggerTokens = asAnalyzedTokenList(word, dictLookup.lookup(word));
      lowerTaggerTokens = asAnalyzedTokenList(word, dictLookup.lookup(lowerWord));
      final boolean isLowercase = word.equals(lowerWord);
      final boolean isMixedcase = StringTools.isMixedCase(word);

      //normal case
      addTokens(taggerTokens, l);

      //tag alluppercase or startuppercase word (but not mixedcase) with lowercase word tags
      if (!isLowercase && !isMixedcase) {        
        addTokens(lowerTaggerTokens, l);
      }

      //tag lowercase word with startuppercase word tags
      if (tagLowercaseWithUppercase) {
        if (lowerTaggerTokens.isEmpty() && taggerTokens.isEmpty()) {
          if (isLowercase) {
            upperTaggerTokens = asAnalyzedTokenList(word,
                dictLookup.lookup(StringTools.uppercaseFirstChar(word)));
            if (!upperTaggerTokens.isEmpty()) {
              addTokens(upperTaggerTokens, l);
            }
          }
        }
      }
      
      if (l.isEmpty()) {
        AnalyzedToken aToken = additionalTag(word);
        if (aToken != null) {
          l.add(aToken);
        }       
      }
      
      if (l.isEmpty()) {
        l.add(new AnalyzedToken(word, null, null));
      }
      
      tokenReadings.add(new AnalyzedTokenReadings(l, pos));
      pos += word.length();
    }

    return tokenReadings;

  }

  protected List<AnalyzedToken> asAnalyzedTokenList(final String word, final List<WordData> wdList) {
    final List<AnalyzedToken> aTokenList = new ArrayList<AnalyzedToken>();
    for (WordData wd : wdList) {
      aTokenList.add(asAnalyzedToken(word, wd));
    }
    return aTokenList;
  }

  protected AnalyzedToken asAnalyzedToken(final String word, final WordData wd) {
    return new AnalyzedToken(
        word,
        StringTools.asString(wd.getTag()), 
        StringTools.asString(wd.getStem()));
  }

  private void addTokens(final List<AnalyzedToken> taggedTokens, final List<AnalyzedToken> l) {
    if (taggedTokens != null) {
      for (AnalyzedToken at : taggedTokens) {
        /*
        if (!StringTools.isEmpty(at.getPOSTag())) {
          l.add(at);
        } else {
          l.add(new AnalyzedToken(at.getToken(), null, null));
        }
        */
        l.add(at);         
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.languagetool.tagging.Tagger#createNullToken(java.lang.String
   * , int)
   */  
  @Override
  public final AnalyzedTokenReadings createNullToken(final String token, final int startPos) {
    return new AnalyzedTokenReadings(new AnalyzedToken(token, null, null), startPos);
  }

  @Override
  public AnalyzedToken createToken(String token, String posTag) {
    return new AnalyzedToken(token, posTag, null);
  }
  
  public void dontTagLowercaseWithUppercase() {
    tagLowercaseWithUppercase=false;
  }
  
  /*
   *  Additional tagging in some language-dependent circumstances
   */
  public AnalyzedToken additionalTag(String word) {
    return null;
  }

}
