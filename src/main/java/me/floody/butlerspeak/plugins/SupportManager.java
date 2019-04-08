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
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import me.floody.butlerspeak.ButlerSpeak;
import me.floody.butlerspeak.config.ConfigNode;
import me.floody.butlerspeak.config.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Notifies clients in specified groups that support is requested.
 */
public class SupportManager extends TS3EventAdapter {

  private final TS3Api api;
  private final Configuration config;
  private final int queryId;

  /** Simply constructs a new instance. */
  public SupportManager(ButlerSpeak plugin) {
	this.api = plugin.getApi();
	this.config = plugin.getConfig();
	this.queryId = plugin.getClientId();
  }

  @Override
  public void onClientMoved(ClientMovedEvent e) {
	final int supportChannel = config.getInt(ConfigNode.SUPPORT_CHANNEL);
	if (e.getTargetChannelId() != supportChannel) {
	  // If the target channel is not the support channel, do nothing.
	  return;
	}

	final int clientId = e.getClientId();
	final Client client;
	try {
	  client = api.getClientInfo(clientId);
	} catch (TS3CommandFailedException ex) {
	  // The client is a query, so do nothing.
	  return;
	}

	final int[] notifyGroups = config.getIntArray(ConfigNode.SUPPORT_NOTIFY_GROUPS);
	for (int group : client.getServerGroups()) {
	  if (IntStream.of(notifyGroups).anyMatch(x -> x == group)) {
		// If an admin enters the support channel, do nothing.
		return;
	  }
	}

	final String notifyAction = config.get(ConfigNode.SUPPORT_NOTIFY_ACTION);
	final String notifyMessage = config.get(ConfigNode.SUPPORT_NOTIFY_MESSAGE)
			.replaceAll("%clientName%", "[URL=" + client.getClientURI() + "]" + client.getNickname() + "[/URL]");
	int pokedClients = 0;
	for (Client notifyClient : api.getClients()) {
	  for (int notifyGroup : notifyGroups) {
		if (!notifyClient.isInServerGroup(notifyGroup)) {
		  // Filter any client that should not be notified
		  continue;
		}

		// Based on the action, the client will either be poked or sent a private message.
		final int notifyClientId = notifyClient.getId();
		if (notifyAction.equals("poke")) {
		  api.pokeClient(notifyClientId, notifyMessage);
		} else if (notifyAction.equals("chat")) {
		  api.sendPrivateMessage(notifyClientId, notifyMessage);
		}

		++pokedClients;
	  }
	}

	if (pokedClients < 1) {
	  final String notifyFailMessage = config.get(ConfigNode.SUPPORT_NOTIFY_FAIL);
	  if (notifyAction.equals("poke")) {
		api.pokeClient(clientId, notifyFailMessage);
	  } else if (notifyAction.equals("chat")) {
		api.sendPrivateMessage(clientId, notifyFailMessage);
	  }

	  // No available client, thus cancel the task.
	  return;
	}

	// When specified, a sub-channel for the requested support will be created
	if (config.getBoolean(ConfigNode.SUPPORT_CREATE_CHANNEL)) {
	  createChannel(supportChannel, client);
	}

	// Based on the action, the client who requested support, will either be poked or sent a message.
	String notifiedMessage = config.get(ConfigNode.SUPPORT_MESSAGE);

	if (notifyAction.equals("poke")) {
	  api.pokeClient(clientId, notifiedMessage);
	} else if (notifyAction.equals("chat")) {
	  api.sendPrivateMessage(clientId, notifyMessage);
	}
  }

  /**
   * Creates a new channel as sub-channel with the client's name and the time the channel was created. This method
   * will only be called when specified in the configuration file.
   *
   * @param parentId
   * 		The channelId of the support channel
   * @param client
   * 		The client who requested support
   */
  private void createChannel(int parentId, Client client) {
	final Map<ChannelProperty, String> channelProperties = new HashMap<>();
	channelProperties.put(ChannelProperty.CHANNEL_FLAG_PERMANENT, "0");
	channelProperties.put(ChannelProperty.CHANNEL_FLAG_MAXCLIENTS_UNLIMITED, "1");
	channelProperties.put(ChannelProperty.CPID, String.valueOf(parentId));

	SimpleDateFormat formattedDate = new SimpleDateFormat();
	formattedDate.applyPattern("hh:mm");
	String channelName = config.get(ConfigNode.SUPPORT_CHANNEL_NAME)
			.replaceAll("%clientName", client.getNickname())
			.replaceAll("%date%", formattedDate.format(new Date()));

	// Create the channel with the desired name and save it's id.
	int createdChannel = api.createChannel(channelName, channelProperties);

	// Finally, move the client to the channel and move the query back to the default channel.
	api.moveClient(client.getId(), createdChannel);
	api.moveClient(queryId, config.getInt(ConfigNode.BOT_CHANNEL));
  }
}
