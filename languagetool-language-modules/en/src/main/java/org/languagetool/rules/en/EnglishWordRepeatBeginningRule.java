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
package org.languagetool.rules.en;

import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.rules.WordRepeatBeginningRule;

/**
 * List of English adverbs for WordRepeatBeginningRule
 * 
 * @author Markus Brenneis
 */
public class EnglishWordRepeatBeginningRule extends WordRepeatBeginningRule {
  
  public EnglishWordRepeatBeginningRule(final ResourceBundle messages, final Language language) {
    super(messages, language);
  }
  
  @Override
  public String getId() {
    return "ENGLISH_WORD_REPEAT_BEGINNING_RULE";
  }
  
  private static final Set<String> ADVERBS = new HashSet<String>();
  static {
    ADVERBS.add("Additionally");
    ADVERBS.add("Besides");
    ADVERBS.add("Furthermore");
    ADVERBS.add("Moreover");
  }
  
  @Override
  protected boolean isAdverb(final AnalyzedTokenReadings token) {
    if (ADVERBS.contains(token.getToken())) return true;
    return false;
  }

}
