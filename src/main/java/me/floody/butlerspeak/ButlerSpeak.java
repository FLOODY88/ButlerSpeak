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

package me.floody.butlerspeak;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import me.floody.butlerspeak.config.ConfigNode;
import me.floody.butlerspeak.config.Configuration;
import me.floody.butlerspeak.plugins.*;

public class ButlerSpeak {

  private final TS3Api api;
  private final Configuration config = new Configuration();
  private static volatile int clientId;

  public static void main(String[] args) {
	new ButlerSpeak();
  }

  /**
   * Constructs a new instance.
   * <p>
   * This constructor sets up the query which is used to communicate with the TeamSpeak3 server.
   * </p><p><i>
   * Note that this constructor is <code>private</code> to prevent initializing new instances from
   * other classes.
   * </i></p>
   */
  protected ButlerSpeak() {
	final TS3Config ts3Config = new TS3Config();
	ts3Config.setHost(config.get(ConfigNode.SERVER_HOST));
	ts3Config.setQueryPort(config.getInt(ConfigNode.QUERY_PORT));
	ts3Config.setFloodRate((config.getBoolean(ConfigNode.BOT_SLOWMODE) ? TS3Query.FloodRate.DEFAULT :
			TS3Query.FloodRate.UNLIMITED));

	final TS3Query query = new TS3Query(ts3Config);
	query.connect();

	// Tries to authenticate the TeamSpeak 3 server. If the connections fails, the application shuts
	// down.
	this.api = query.getApi();
	try {
	  api.login(config.get(ConfigNode.QUERY_USERNAME), config.get(ConfigNode.QUERY_PASSWORD));
	} catch (TS3CommandFailedException e) {
	  e.printStackTrace();
	  System.exit(0);
	}

	api.selectVirtualServerByPort(config.getInt(ConfigNode.SERVER_PORT),
			config.get(ConfigNode.BOT_USERNAME));
	api.registerAllEvents();

	loadPlugins();
	clientId = api.whoAmI().getId();
  }

  /**
   * Loads all plugins that should be enabled specified by the configuration file
   */
  private void loadPlugins() {
	for (String plugin : config.getStringArray(ConfigNode.BOT_PLUGINS)) {
	  switch (plugin.toLowerCase()) {
		case "welcome":
		  api.addTS3Listeners(new WelcomeMessage(this));
		  break;
		case "afk":
		  api.addTS3Listeners(new AfkManager(this));
		  break;
		case "badname":
		  api.addTS3Listeners(new NameChecker(this));
		  break;
		case "recording":
		  api.addTS3Listeners(new RecordingChecker(this));
		  break;
		case "support":
		  api.addTS3Listeners(new SupportManager(this));
		  break;
		case "advertisement":
		  new Advertisement(this);
		  break;
		default:
		  break;
	  }
	}
  }

  /** Returns the client id for the query. */
  public int getClientId() {
	return clientId;
  }

  /** Returns the {@link com.github.theholywaffle.teamspeak3.TS3Api} object. Used to interact with the server. */
  public TS3Api getApi() {
	return api;
  }

  /**
   * Returns the {@link me.floody.butlerspeak.config.Configuration} object. Used the retrieve the configuration's
   * values.
   */
  public Configuration getConfig() {
	return config;
  }
}