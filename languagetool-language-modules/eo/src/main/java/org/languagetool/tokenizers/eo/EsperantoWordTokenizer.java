/* LanguageTool, a natural language style checker 
 * Copyright (C) 2008 Daniel Naber (http://www.danielnaber.de)
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

package org.languagetool.tokenizers.eo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.languagetool.tokenizers.WordTokenizer;

/** 
 * @author Dominique Pelle
 */
public class EsperantoWordTokenizer extends WordTokenizer {

  public EsperantoWordTokenizer() {
  }

  /**
   * Tokenizes just like WordTokenizer with the exception that words
   * such as "dank'" contain an apostrophe within it.
   * 
   * @param text
   *          - Text to tokenize
   * @return List of tokens.
   * 
   *         Note: a special string ##EO_APOS## is used to replace apostrophe
   *         during tokenizing.
   */
  @Override
  public List<String> tokenize(final String text) {
    // TODO: find a cleaner implementation, this is a hack
    
    String replaced = text.replaceAll(
      "(?<!')\\b([a-zA-ZĉĝĥĵŝŭĈĜĤĴŜŬ]+)'(?![a-zA-ZĉĝĥĵŝŭĈĜĤĴŜŬ-])",
      "$1##EO_APOS1##").replaceAll(
      "(?<!')\\b([a-zA-ZĉĝĥĵŝŭĈĜĤĴŜŬ]+)'(?=[a-zA-ZĉĝĥĵŝŭĈĜĤĴŜŬ-])",
      "$1##EO_APOS2## ");
    final List<String> tokenList = super.tokenize(replaced);
    List<String> tokens = new ArrayList<String>();

    // Put back apostrophes and remove spurious spaces.
    Iterator<String> itr = tokenList.iterator();
    while (itr.hasNext()) {
      String word = itr.next();
      if (word.endsWith("##EO_APOS2##")) {
        itr.next(); // Skip the next spurious white space.
      }
      word = word.replace("##EO_APOS1##", "'")
                 .replace("##EO_APOS2##", "'");
      tokens.add(word);
    }
    return tokens;
  }
}
