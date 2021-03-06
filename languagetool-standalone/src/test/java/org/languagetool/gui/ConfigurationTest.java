/* LanguageTool, a natural language style checker 
 * Copyright (C) 2011 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.gui;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.language.Belarusian;

public class ConfigurationTest extends TestCase {

  public void testSaveAndLoadConfiguration() throws Exception {
    final File tempFile = File.createTempFile(ConfigurationTest.class.getSimpleName(), ".cfg");
    createConfiguration(tempFile, null);
    try {
      final Configuration conf = new Configuration(tempFile.getParentFile(), tempFile.getName(), null);
      final Set<String> disabledRuleIds = conf.getDisabledRuleIds();
      assertTrue(disabledRuleIds.contains("FOO1"));
      assertTrue(disabledRuleIds.contains("Foo2"));
      assertEquals(2, disabledRuleIds.size());
      final Set<String> enabledRuleIds = conf.getEnabledRuleIds();
      assertTrue(enabledRuleIds.contains("enabledRule"));
      assertEquals(1, enabledRuleIds.size());
    } finally {
      tempFile.delete();
    }
  }

  private void createConfiguration(File configFile, Language lang) throws Exception {
    final Configuration conf = new Configuration(configFile.getParentFile(), configFile.getName(), lang);
    conf.setDisabledRuleIds(new HashSet<String>(Arrays.asList("FOO1", "Foo2")));
    conf.setEnabledRuleIds(new HashSet<String>(Arrays.asList("enabledRule")));
    conf.saveConfiguration(lang);
  }

  public void testSaveAndLoadConfigurationForManyLanguages() throws Exception {
    final File tempFile = File.createTempFile(ConfigurationTest.class.getSimpleName(), ".cfg");
    createConfiguration(tempFile, new AmericanEnglish());
    try {
      Configuration conf = new Configuration(tempFile.getParentFile(), tempFile.getName(),
              new AmericanEnglish());
      Set<String> disabledRuleIds = conf.getDisabledRuleIds();
      assertTrue(disabledRuleIds.contains("FOO1"));
      assertTrue(disabledRuleIds.contains("Foo2"));
      assertEquals(2, disabledRuleIds.size());
      Set<String> enabledRuleIds = conf.getEnabledRuleIds();
      assertTrue(enabledRuleIds.contains("enabledRule"));
      assertEquals(1, enabledRuleIds.size());

      //now change language

      conf = new Configuration(tempFile.getParentFile(), tempFile.getName(),
              new Belarusian());
      disabledRuleIds = conf.getDisabledRuleIds();
      assertTrue(disabledRuleIds.isEmpty());
      enabledRuleIds = conf.getEnabledRuleIds();
      assertTrue(enabledRuleIds.isEmpty());

      conf.setEnabledRuleIds(new HashSet<String>(Arrays.asList("enabledBYRule")));
      conf.saveConfiguration(new Belarusian());

      //and back...
      conf = new Configuration(tempFile.getParentFile(), tempFile.getName(),
              new AmericanEnglish());
      disabledRuleIds = conf.getDisabledRuleIds();
      assertTrue(disabledRuleIds.contains("FOO1"));
      assertTrue(disabledRuleIds.contains("Foo2"));
      assertEquals(2, disabledRuleIds.size());
      enabledRuleIds = conf.getEnabledRuleIds();
      assertTrue(enabledRuleIds.contains("enabledRule"));
      assertEquals(1, enabledRuleIds.size());


    } finally {
      tempFile.delete();
    }
  }

}
