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
package org.languagetool.rules.spelling.hunspell;

import de.abelssoft.wordtools.jwordsplitter.AbstractWordSplitter;
import org.languagetool.Language;
import org.languagetool.rules.spelling.morfologik.MorfologikSpeller;
import org.languagetool.tools.StringTools;

import java.io.IOException;
import java.util.*;

/**
 * A spell checker that combines Hunspell und Morfologik spell checking
 * to support compound words and offer fast suggestions for some misspelled
 * compound words.
 */
abstract public class CompoundAwareHunspellRule extends HunspellRule {

  private final static int MAX_SUGGESTIONS = 20;
  
  private final AbstractWordSplitter wordSplitter;
  private final MorfologikSpeller morfoSpeller;
  
  public CompoundAwareHunspellRule(ResourceBundle messages, Language language, AbstractWordSplitter wordSplitter, MorfologikSpeller morfoSpeller) {
    super(messages, language);
    this.wordSplitter = wordSplitter;
    this.morfoSpeller = morfoSpeller;
  }

  /**
   * As a hunspell-based approach is too slow, we use Morfologik to create suggestions. As this
   * won't work for compounds not in the dictionary, we split the word and also get suggestions
   * on the compound parts. In the end, all candidates are filtered against Hunspell again (which 
   * supports compounds).
   */
  @Override
  public List<String> getSuggestions(String word) throws IOException {
    if (needsInit) {
      init();
    }
    final List<String> candidates = new ArrayList<String>();
    
    final List<String> noSplitSuggestions = morfoSpeller.getSuggestions(word);
    candidates.addAll(noSplitSuggestions);

    if (StringTools.startsWithUppercase(word) && !StringTools.isAllUppercase(word)) {
      // almost all words can be uppercase because they can appear at the start of a sentence:
      final List<String> noSplitLowercaseSuggestions = morfoSpeller.getSuggestions(word.toLowerCase());
      int pos = 0;
      for (String suggestion : noSplitLowercaseSuggestions) {
        candidates.add(pos, StringTools.uppercaseFirstChar(suggestion));
        // we don't know about the quality of the results here, so mix both lists together, taking
        // taking elements from both lists on a rotating basis: 
        pos = Math.min(pos + 2, candidates.size());
      }
    }

    final Collection<String> parts = wordSplitter.splitWord(word);
    int partCount = 0;
    for (String part : parts) {
      if (dictionary.misspelled(part)) {
        List<String> suggestions = morfoSpeller.getSuggestions(part);
        if (suggestions.size() == 0) {
          suggestions = morfoSpeller.getSuggestions(StringTools.uppercaseFirstChar(part));
        }
        for (String suggestion : suggestions) {
          final List<String> partsCopy = new ArrayList<String>(parts);
          partsCopy.set(partCount, suggestion);
          candidates.add(StringTools.listToString(partsCopy, ""));
        }
      }
      // TODO: what if there's no misspelled parts like for Arbeitamt = Arbeit+Amt ??
      // -> morfologik must be extended to return similar words even for known words
      partCount++;
    }
    filterDupes(candidates);
    final List<String> correctWords = getCorrectWords(candidates);
    return correctWords.subList(0, Math.min(MAX_SUGGESTIONS, correctWords.size()));
  }
  
  private void filterDupes(List<String> words) {
    final Set<String> seen = new HashSet<String>();
    final Iterator<String> iterator = words.iterator();
    while (iterator.hasNext()) {
      final String word = iterator.next();
      if (seen.contains(word)) {
        iterator.remove();
      }
      seen.add(word);
    }
  }
  
  private List<String> getCorrectWords(List<String> wordsOrPhrases) {
    final List<String> result = new ArrayList<String>();
    for (String wordOrPhrase : wordsOrPhrases) {
      // this might be a phrase like "aufgrund dessen", so it needs to be split: 
      final String[] words = tokenizeText(wordOrPhrase);
      boolean wordIsOkay = true;
      for (String word : words) {
        if (dictionary.misspelled(word)) {
          wordIsOkay = false;
          break;
        }
      }
      if (wordIsOkay) {
        result.add(wordOrPhrase);
      }
    }
    return result;
  }

}
