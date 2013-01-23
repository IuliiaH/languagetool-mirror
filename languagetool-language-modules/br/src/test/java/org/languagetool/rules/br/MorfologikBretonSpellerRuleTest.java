package org.languagetool.rules.br;

import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.TestTools;
import org.languagetool.language.Breton;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MorfologikBretonSpellerRuleTest {

    @Test
    public void testMorfologikSpeller() throws IOException {
        MorfologikBretonSpellerRule rule =
                new MorfologikBretonSpellerRule (TestTools.getMessages("Breton"), new Breton());

        RuleMatch[] matches;
        JLanguageTool langTool = new JLanguageTool(new Breton());


        // correct sentences:
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("Penaos emañ kont ganit?")).length);
        
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("C'hwerc'h merc'h gwerc'h war c'hwerc'h marc'h kalloc'h")).length);
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("C’hwerc’h merc’h gwerc‘h war c‘hwerc‘h marc'h kalloc‘h")).length);
        
        //words with hyphens are tokenized internally...
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("Evel-just")).length);
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("Barrek-tre eo LanguageTool")).length);
        
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("C'hwerc'h merc'h gwerc'h war c'hwerc'h marc'h kalloc'h")).length);
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("C’hwerc’h merc’h gwerc‘h war c‘hwerc‘h marc'h kalloc‘h")).length);
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("Evel-just")).length);
        assertEquals(1, rule.match(langTool.getAnalyzedSentence("Evel-juste")).length);
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("Barrek-tre eo LanguageTool")).length);

        //test for "LanguageTool":
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("LanguageTool!")).length);
        assertEquals(0, rule.match(langTool.getAnalyzedSentence(",")).length);
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("123454")).length);

        //incorrect sentences:

        assertEquals(1, rule.match(langTool.getAnalyzedSentence("Evel-juste")).length);

        matches = rule.match(langTool.getAnalyzedSentence("Evel-juste"));

        // check match positions:
        assertEquals(1, matches.length);
        assertEquals(5, matches[0].getFromPos());
        assertEquals(10, matches[0].getToPos());               

        matches = rule.match(langTool.getAnalyzedSentence("C’hreizhig-don"));

        assertEquals(1, matches.length);
        
        // check match positions:
        assertEquals(1, matches.length);
        assertEquals(0, matches[0].getFromPos());
        assertEquals(10, matches[0].getToPos());

        
        assertEquals(1, rule.match(langTool.getAnalyzedSentence("aõh")).length);
        assertEquals(0, rule.match(langTool.getAnalyzedSentence("a")).length);

    }

}
