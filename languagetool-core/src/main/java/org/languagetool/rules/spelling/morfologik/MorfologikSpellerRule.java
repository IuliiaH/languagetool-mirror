/* LanguageTool, a natural language style checker 
 * Copyright (C) 2012 Marcin Miłkowski (http://www.languagetool.org)
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

package org.languagetool.rules.spelling.morfologik;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.rules.Category;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;
import org.languagetool.AnalyzedToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;
import java.text.Normalizer.Form;

public abstract class MorfologikSpellerRule extends SpellingCheckRule {

  private MorfologikSpeller speller;
  private Locale conversionLocale;
  private boolean ignoreTaggedWords=false;

  /**
   * Get the filename, e.g., <tt>/resource/pl/spelling.dict</tt>.
   */
  public abstract String getFileName();

  public MorfologikSpellerRule(ResourceBundle messages, Language language) throws IOException {
    super(messages, language);
    super.setCategory(new Category(messages.getString("category_typo")));
    this.conversionLocale = conversionLocale != null ? conversionLocale : Locale.getDefault();
    init();
  }

  @Override
  public abstract String getId();

  @Override
  public String getDescription() {
    return messages.getString("desc_spelling");
  }

  public void setLocale(Locale locale) {
    conversionLocale = locale;
  }

  @Override
  public RuleMatch[] match(AnalyzedSentence text) throws IOException {
    final List<RuleMatch> ruleMatches = new ArrayList<RuleMatch>();
    final AnalyzedTokenReadings[] tokens = text.getTokensWithoutWhitespace();
    //lazy init
    if (speller == null) {
      if (JLanguageTool.getDataBroker().resourceExists(getFileName())) {
        speller = new MorfologikSpeller(getFileName(), conversionLocale);
      } else {
        // should not happen, as we only configure this rule (or rather its subclasses)
        // when we have the resources:
        return toRuleMatchArray(ruleMatches);
      }
    }
    skip:
    for (AnalyzedTokenReadings token : tokens) {
      if (isUrl(token.getToken())) {
        continue;
      }
      final String word = token.getToken();
      if (ignoreWord(word) || token.isImmunized()) {
        continue;
      }
      if (ignoreTaggedWords) {
        for (AnalyzedToken at : token.getReadings()) {
          if (!at.hasNoTag())
            continue skip; // if it HAS a POS tag then it is a known word.
        }
      }
      if (tokenizingPattern() == null) {
        ruleMatches.addAll(getRuleMatch(word, token.getStartPos()));
      } else {
        int index = 0;
        final Matcher m = tokenizingPattern().matcher(word);
        while (m.find()) {
          final String match = word.subSequence(index, m.start()).toString();
          ruleMatches.addAll(getRuleMatch(match, token.getStartPos() + index));
          index = m.end();
        }
        if (index == 0) { // tokenizing char not found
          ruleMatches.addAll(getRuleMatch(word, token.getStartPos()));
        } else {
          ruleMatches.addAll(getRuleMatch(word.subSequence(
                  index, word.length()).toString(), token.getStartPos() + index));
        }
      }
    }
    return toRuleMatchArray(ruleMatches);
  }

  private List<RuleMatch> getRuleMatch(final String word, final int startPos) {
    final List<RuleMatch> ruleMatches = new ArrayList<RuleMatch>();
    if (speller.isMisspelled(word)) {
      final RuleMatch ruleMatch = new RuleMatch(this, startPos, startPos
          + word.length(), messages.getString("spelling"),
          messages.getString("desc_spelling_short")); 
      //If lower case word is not a misspelled word, return it as the only suggestion 
      if (!speller.isMisspelled(word.toLowerCase(conversionLocale))) {
        List<String> suggestion = Arrays.asList(word.toLowerCase(conversionLocale));
        ruleMatch.setSuggestedReplacements(suggestion);
        ruleMatches.add(ruleMatch);
        return ruleMatches;
      }
      List<String> suggestions = speller.getSuggestions(word);
      //If few suggestions are found, try to get more from the word without diacritics and lowercase
      final String wordWithoutDiacritics=removeAccents(word).toLowerCase(conversionLocale);
      if (suggestions.size() < 5 && !word.equals(wordWithoutDiacritics)) {
        List<String> moreSuggestions = speller.getSuggestions(wordWithoutDiacritics);
        if (!speller.isMisspelled(wordWithoutDiacritics)) {
          moreSuggestions.add(wordWithoutDiacritics);
        }
        for (int i = 0; i < moreSuggestions.size(); i++) {
          if (!suggestions.contains(moreSuggestions.get(i))) {
            suggestions.add(moreSuggestions.get(i));
          }
        }
      }
      if (!suggestions.isEmpty()) {
        ruleMatch.setSuggestedReplacements(suggestions);
      }
      ruleMatches.add(ruleMatch);
    }
    return ruleMatches;
  }

  /**
   * Get the regular expression pattern used to tokenize
   * the words as in the source dictionary. For example,
   * it may contain a hyphen, if the words with hyphens are
   * not included in the dictionary
   * @return A compiled {@link Pattern} that is used to tokenize words or null.
   */
  public Pattern tokenizingPattern() {
    return null;
  }

  public void setIgnoreTaggedWords() {
    ignoreTaggedWords=true;
  }
  
  /*
   * Remove all diacritical marks from a String
   */
  private static String removeAccents(String text) {
    return text == null ? null
        : Normalizer.normalize(text, Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }

}
