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
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import me.floody.butlerspeak.ButlerSpeak;
import me.floody.butlerspeak.config.ConfigNode;
import me.floody.butlerspeak.config.Configuration;

/**
 * Sends a message to clients connecting to the TeamSpeak3 server.
 * <p>
 * Before sending the message, various configuration properties will be checked. These properties
 * can be adjusted by editing the <code>ButlerSpeak.properties</code> file.
 * </p>
 */
public class WelcomeMessage extends TS3EventAdapter {

  private final TS3Api api;
  private final Configuration config;

  /** Simply constructs a new instance. */
  public WelcomeMessage(ButlerSpeak plugin) {
	this.api = plugin.getApi();
	this.config = plugin.getConfig();
  }

  @Override
  public void onClientJoin(ClientJoinEvent e) {
	final int clientId = e.getClientId();
	final Client client;
	try {
	  client = api.getClientInfo(clientId);
	} catch (TS3CommandFailedException ex) {
	  // The task failed because the client's a query, so do nothing.
	  return;
	}

	final ClientInfo clientInfo = api.getClientInfo(clientId);
	final int configConnections = config.getInt(ConfigNode.WELCOME_CONNECTIONS);
	// Checks whether the client exceeded the amount of connections needed to receive the
	// welcome message if and only if not all clients should receive it.
	if (configConnections != -1 && clientInfo.getTotalConnections() > configConnections) {
	  return;
	}

	final int[] configGroups = config.getIntArray(ConfigNode.WELCOME_GROUPS);
	if (configGroups[0] != -1) {
	  // If and only if groups should be excluded from receiving welcome messages, check
	  // whether the client is in any of these groups.
	  for (int configGroup : configGroups) {
		if (client.isInServerGroup(configGroup)) {
		  // If the client is in a server group which is excluded from receiving welcome
		  // messages, do nothing.
		  return;
		}
	  }
	}

	// Replace all provided placeholders with the corresponding value.
	String message = config.get(ConfigNode.WELCOME_MESSAGE)
			.replaceAll("%clientName%", client.getNickname())
			.replaceAll("%clientIP%", client.getIp())
			.replaceAll("%clientCountry%", client.getCountry())
			.replaceAll("%totalConnections%",
					String.valueOf(clientInfo.getTotalConnections()))
			.replaceAll("%lastConnection%", client.getLastConnectedDate().toString());

	// Finally, send the welcome message to the client.
	api.sendPrivateMessage(e.getClientId(), message);
  }
}
