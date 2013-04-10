/* LanguageTool, a natural language style checker 
 * Copyright (C) 2013 Daniel Naber (http://www.danielnaber.de)
 *                    Paolo Bianchini
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
package org.languagetool.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.tools.StringTools;

/**
 * A rule that matches several punctuation signs such as : ; and % preceded by whitespace.
 * 
 * BUG ID 3607406: no space before semicolon
 * 
 * Checks for: 
 * 				<a word> : and suggests <a word>:
 * 				<a word> ; and suggests <a word>;
 * 				<a number> % and suggests <a number>%
 * 
 * @author Paolo Bianchini
 */

public class WhitespaceBeforePunctuationRule extends Rule {

  public WhitespaceBeforePunctuationRule(final ResourceBundle messages) {
    super(messages);
    super.setCategory(new Category(messages.getString("category_misc")));
    setLocQualityIssueType("typographical");
  }

  @Override
  public final String getId() {
    return "WHITESPACE_PUNCTUATION";
  }

  @Override
  public final String getDescription() {
    return messages.getString("desc_whitespace_before_punctuation");
  }

  @Override
  public final RuleMatch[] match(final AnalyzedSentence text) {
    final List<RuleMatch> ruleMatches = new ArrayList<RuleMatch>();
    final AnalyzedTokenReadings[] tokens = text.getTokens();
    String prevToken = "";
    String prevPrevToken = "";
    boolean prevWhite = false;    
    int prevLen = 0;
    for (int i = 0; i < tokens.length; i++) {
      final String token = tokens[i].getToken();
      final boolean isWhitespace = tokens[i].isWhitespace() || StringTools.isNonBreakingWhitespace(token)
              || tokens[i].isFieldCode();
      String msg = null;
      String suggestionText = null;
      if (prevWhite) {
        if (token.equals(":")) {
            msg = messages.getString("no_space_before_colon");
            suggestionText = ":";
            // exception case for figures such as " : 0" 
            if (i + 2 < tokens.length
                && tokens[i + 1].isWhitespace()
            	&& Character.isDigit(tokens[i + 2].getToken().charAt(0))) {
              msg = null;
            }
        } else if (token.equals(";")) {
            msg = messages.getString("no_space_before_semicolon");
            suggestionText = ";";
        } else if (i > 1
        		&& token.equals("%")
        		&& Character.isDigit(tokens[i - 2].getToken().charAt(0))) {
            msg = messages.getString("no_space_before_percentage");
            suggestionText = "%";
        }
      }
      if (msg != null) {
        final int fromPos = tokens[i - 1].getStartPos();
        final int toPos = tokens[i - 1].getStartPos() + 1 + prevLen;
        // TODO: add some good short comment here
        final RuleMatch ruleMatch = new RuleMatch(this, fromPos, toPos, msg);
        ruleMatch.setSuggestedReplacement(suggestionText);
        ruleMatches.add(ruleMatch);
      }
      prevPrevToken = prevToken;
      prevToken = token;
      prevWhite = isWhitespace && !tokens[i].isFieldCode(); //OOo code before comma/dot
      prevLen = tokens[i].getToken().length();
    }

    return toRuleMatchArray(ruleMatches);
  }

  @Override
  public void reset() {
    // nothing
  }

}
