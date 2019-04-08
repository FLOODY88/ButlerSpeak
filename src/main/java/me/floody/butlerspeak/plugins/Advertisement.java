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

package me.floody.butlerspeak.plugins;

import com.github.theholywaffle.teamspeak3.TS3Api;
import me.floody.butlerspeak.ButlerSpeak;
import me.floody.butlerspeak.config.ConfigNode;
import me.floody.butlerspeak.config.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Broadcasts a message to the whole server every X minutes.
 */
public class Advertisement implements Runnable {

  private final TS3Api api;
  private final Configuration config;
  private final ScheduledExecutorService executor;

  /**
   * Constructs a new instance and schedules the task to broadcast a message to the server.
   */
  public Advertisement(ButlerSpeak plugin) {
	this.api = plugin.getApi();
	this.config = plugin.getConfig();
	this.executor = new ScheduledThreadPoolExecutor(1);

	// First message should be sent after the specified delay.
	executor.schedule(this, config.getLong(ConfigNode.ADVERTISEMENT_DELAY), TimeUnit.MINUTES);
  }

  @Override
  public void run() {
	api.sendServerMessage(config.get(ConfigNode.ADVERTISEMENT_MESSAGE));

	// Once the message was sent, reschedule the task.
	executor.schedule(this, config.getLong(ConfigNode.ADVERTISEMENT_DELAY), TimeUnit.MINUTES);
  }
}
