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
package org.languagetool.rules.patterns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.languagetool.AnalyzedSentence;
import org.languagetool.Language;
import org.languagetool.rules.RuleMatch;
import org.languagetool.tools.StringTools;

/**
 * A Rule that describes a language error as a simple pattern of words or of
 * part-of-speech tags.
 * 
 * @author Daniel Naber
 */
public class PatternRule extends AbstractPatternRule {

  private final String shortMessage;

  /**
   * A list of elements as they appear in XML file (phrases count as single
   * tokens in case of matches or skipping).
   */
  private final List<Integer> elementNo;

  private String message;
  private String suggestionsOutMsg; // extra suggestions outside message

  /** Formatted suggestion elements. **/
  private List<Match> suggestionMatches;
  
  /** Formatted suggestion elements outside message. **/
  private List<Match> suggestionMatchesOutMsg;

  /**
   * This property is used for short-circuiting evaluation of the elementNo list
   * order.
   */
  private boolean useList;

  /**
   * Marks whether the rule is a member of a disjunctive set (in case of OR
   * operation on phraserefs).
   **/
  private boolean isMemberOfDisjunctiveSet;

  /**
   * @param id
   *          Id of the Rule. Used in configuration. Should not contain special characters and should
   *          be stable over time, unless the rule changes completely.
   * @param language
   *          Language of the Rule
   * @param elements
   *          Element (token) list
   * @param description
   *          Description to be shown (name)
   * @param message
   *          Message to be displayed to the user
   * @param shortMessage
   *          Message to be displayed to the user in the context menu in OpenOffice.org/LibreOffice
   */
  public PatternRule(final String id, final Language language,
      final List<Element> elements, final String description,
      final String message, final String shortMessage) {
    super(id, description, language, elements, false);
    if (id == null) throw new NullPointerException("id cannot be null");
    if (language == null) throw new NullPointerException("language cannot be null");
    if (elements == null) throw new NullPointerException("elements cannot be null");
    if (description == null) throw new NullPointerException("description/name cannot be null");
    this.message = message;
    this.shortMessage = shortMessage;
    this.elementNo = new ArrayList<Integer>();
    this.suggestionsOutMsg="";
    String prevName = "";
    String curName = "";
    int cnt = 0;
    int loopCnt = 0;
    for (final Element e : patternElements) {
      if (e.isPartOfPhrase()) {
        curName = e.getPhraseName();
        if (prevName.equals(curName) || StringTools.isEmpty(prevName)) {
          cnt++;
          useList = true;
        } else {
          elementNo.add(cnt);
          curName = "";
          cnt = 0;
        }
        prevName = curName;
        loopCnt++;
        if (loopCnt == patternElements.size() && !StringTools.isEmpty(prevName)) {
          elementNo.add(cnt);
        }
      } else {
        if (cnt > 0) {
          elementNo.add(cnt);
        }
        elementNo.add(1);
        loopCnt++;
      }
    }    
  }  
  
  public PatternRule(final String id, final Language language,
      final List<Element> elements, final String description,
      final String message, final String shortMessage, final String suggestionsOutMsg) {
    this(id, language, elements, description, message, shortMessage);
    this.suggestionsOutMsg=suggestionsOutMsg;
  }

  public PatternRule(final String id, final Language language,
      final List<Element> elements, final String description,
      final String message, final String shortMessage, final String suggestionsOutMsg,
      final boolean isMember) {
    this(id, language, elements, description, message, shortMessage, suggestionsOutMsg);
    this.isMemberOfDisjunctiveSet = isMember;
  }  

  public final String getMessage() {
    return message;
  }
  
  public final String getSuggestionsOutMsg() {
    return suggestionsOutMsg;
  }

  /**
   * Used for testing rules: only one of the set can match.
   * 
   * @return Whether the rule can non-match (as a member of disjunctive set of
   *         rules generated by phraseref in includephrases element).
   */
  public final boolean isWithComplexPhrase() {
    return isMemberOfDisjunctiveSet;
  }

  /** Reset complex status - used for testing. **/
  public final void notComplexPhrase() {
    isMemberOfDisjunctiveSet = false;
  }

  /**
   * Return the pattern as a string.
   * 
   * @since 0.9.2
   */
  public final String toPatternString() {
    final List<String> strList = new ArrayList<String>();
    for (Element patternElement : patternElements) {
      strList.add(patternElement.toString());
    }
    return StringTools.listToString(strList, ", ");
  }

  /**
   * Return the pattern as an XML string. FIXME: this is not complete, information might be lost!
   * 
   * @since 0.9.3
   */
  public final String toXML() {
    final PatternRuleXmlCreator xmlCreator = new PatternRuleXmlCreator();
    return xmlCreator.toXML(this);
  }

  public final void setMessage(final String message) {
    this.message = message;
  }

  @Override
  public final RuleMatch[] match(final AnalyzedSentence text) throws IOException {
    final PatternRuleMatcher matcher = new PatternRuleMatcher(this, useList);
    return matcher.match(text);
  }

  public final void addSuggestionMatch(final Match m) {
    if (suggestionMatches == null) {
      suggestionMatches = new ArrayList<Match>();
    }
    suggestionMatches.add(m);
  }
  
  public final void addSuggestionMatchOutMsg (final Match m) {
    if (suggestionMatchesOutMsg == null) {
      suggestionMatchesOutMsg = new ArrayList<Match>();
    }
    suggestionMatchesOutMsg.add(m);
  }
  
  /**
   * For testing only.
   */
  public final List<Element> getElements() {
    return patternElements;
  }

  List<Integer> getElementNo() {
    return elementNo;
  }

  String getShortMessage() {
    return shortMessage;
  }
  
  List<Match> getSuggestionMatches() {
    return suggestionMatches;
  }
  
  List<Match> getSuggestionMatchesOutMsg() {
    return suggestionMatchesOutMsg;
  }

}
