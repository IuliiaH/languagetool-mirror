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
package org.languagetool.rules.de;

import junit.framework.TestCase;
import org.languagetool.JLanguageTool;
import org.languagetool.language.German;

import java.io.IOException;

public class UppercaseSentenceStartRuleTest extends TestCase {

  public void testRule() throws IOException {
    final JLanguageTool lt = new JLanguageTool(new German());
    
    assertEquals(0, lt.check("Dies ist ein Satz. Und hier kommt noch einer").size());
    assertEquals(0, lt.check("Dies ist ein Satz. Ätsch, noch einer mit Umlaut.").size());
    assertEquals(0, lt.check("Dieser Satz ist bspw. okay so.").size());
    assertEquals(0, lt.check("Dieser Satz ist z.B. okay so.").size());
    assertEquals(0, lt.check("Dies ist ein Satz. \"Aber der hier auch!\".").size());
    assertEquals(0, lt.check("\"Dies ist ein Satz!\"").size());
    assertEquals(0, lt.check("'Dies ist ein Satz!'").size());
    
    assertEquals(0, lt.check("Sehr geehrte Frau Merkel,\nwie wir Ihnen schon früher mitgeteilt haben...").size());
    assertEquals(0, lt.check("Dies ist ein Satz. aber das hier noch nicht").size());
    
    assertEquals(1, lt.check("Dies ist ein Satz. ätsch, noch einer mit Umlaut.").size());
    assertEquals(1, lt.check("Dies ist ein Satz. \"aber der hier auch!\"").size());
    assertEquals(1, lt.check("Dies ist ein Satz. „aber der hier auch!“").size());
    assertEquals(1, lt.check("\"dies ist ein Satz!\"").size());
    assertEquals(1, lt.check("'dies ist ein Satz!'").size());
  }

}
