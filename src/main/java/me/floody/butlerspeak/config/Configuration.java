/*
 * ButlerSpeak - TeamSpeak 3 Server Query Bot
 * Copyright (C) 2019 FLOODY88 (https://github.com/FLOODY88)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.floody.butlerspeak.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Configuration handler for application. Access data via various getter methods.
 */
public class Configuration {

  private final Properties properties;

  /**
   * Constructs a new instance.
   * <p>
   * This constructor loads the configuration file <code>ButlerSpeak.properties</code> and
   * tries to find it in the local directory. If not found, the file will be copied from the
   * local resource folder and will be placed in the same directory the jar is located.
   * </p>
   */
  public Configuration() {
	final File file = new File("ButlerSpeak.properties");

	if (!file.exists()) {
	  try (InputStream in = Configuration.class.getResourceAsStream("/ButlerSpeak_EXAMPLE.properties")) {
		Files.copy(in, file.toPath());
		// Shutdown since the bot cannot connect to the server without an adjusted configuration file.
		System.exit(0);
	  } catch (IOException e) {
		e.printStackTrace();
	  }
	}

	this.properties = new Properties();
	try {
	  properties.load(new FileInputStream(file));
	} catch (IOException e) {
	  e.printStackTrace();
	  System.exit(0);
	}
  }

  /** Returns the property's value as <code>String</code>. */
  public String get(ConfigNode node) {
	return properties.getProperty(node.getKey());
  }

  /** Returns the property's value as <code>Integer</code>. */
  public int getInt(ConfigNode node) {
	return Integer.parseInt(properties.getProperty(node.getKey()));
  }

  /** Returns the property's value as <code>Long</code>. */
  public long getLong(ConfigNode node) {
	return Long.parseLong(properties.getProperty(node.getKey()));
  }

  /** Returns the property's value as <code>Boolean</code>. */
  public boolean getBoolean(ConfigNode node) {
	return Boolean.parseBoolean(properties.getProperty(node.getKey()));
  }

  /**
   * Returns the property's value as a String array.
   * <p>
   * This method also removes all whitespaces and splits the value at any comma.
   * </p>
   */
  public String[] getStringArray(ConfigNode node) {
	return properties.getProperty(node.getKey()).replaceAll("\\s+", "").split(",");
  }

  /**
   * Returns the property's value as a Integer array.
   *
   * @see #getStringArray(ConfigNode) Configuration#getStringArray
   */
  public int[] getIntArray(ConfigNode node) {
	String[] tempArr = getStringArray(node);
	int[] tempIntArr = new int[tempArr.length];
	for (int i = 0; i < tempArr.length; i++) {
	  tempIntArr[i] = Integer.parseInt(tempArr[i]);
	}

	return tempIntArr;
  }
}
