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

package me.floody.butlerspeak.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logs certain events in other classes.
 * <p>
 * All logs will be saved to {@code logs/butlerspeak.log}. Every logfile will be saved when a new file is
 * generated.
 * </p>
 */
public class Log {

  private final Logger logger;

  /**
   * Initializes a new instance for the given class' name.
   * <p>
   * The {@code name} will be displayed as source.
   * </p>
   *
   * @param name
   * 		the name of the class
   */
  public Log(String name) {
	this.logger = LogManager.getLogger(name);
  }

  public void error(String message) {
	logger.error(message);
  }

  public void error(String message, Throwable t) {
	logger.error(message, t);
  }

  public void info(String message) {
	logger.info(message);
  }
}
