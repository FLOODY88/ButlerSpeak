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
import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import me.floody.butlerspeak.ButlerSpeak;
import me.floody.butlerspeak.config.ConfigNode;
import me.floody.butlerspeak.config.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Checks channel and client's name for forbidden words.
 */
public class NameChecker extends TS3EventAdapter {

  private final TS3Api api;
  private final Configuration config;
  private final ScheduledExecutorService executor;
  private final Map<Integer, CheckClientName> workers;

  /**
   * Initializes a new instance.
   * <p>
   * When first initialized, all clients and channels will be if they contain any forbidden words that matches the
   * specified pattern. Also, it listens to {@link ClientJoinEvent}, {@link ChannelEditedEvent} and
   * {@link ChannelCreateEvent} to check the name upon changes.
   * </p>
   */
  public NameChecker(ButlerSpeak plugin) {
	this.api = plugin.getApi();
	this.config = plugin.getConfig();
	this.executor = new ScheduledThreadPoolExecutor(1);
	this.workers = new HashMap<>();

	// On first start, check the existing channels for bad names.
	for (Channel channel : api.getChannels()) {
	  int channelId = channel.getId();
	  for (int needle : config.getIntArray(ConfigNode.BADNAME_CHANNEL)) {
		if (channelId == needle) {
		  // Filter any channel that should be ignored.
		  continue;
		}

		checkChannel(channelId);
	  }
	}

	// On first start, check connected clients for bad names.
	for (Client client : api.getClients()) {
	  if (!client.isRegularClient()) {
		// Filter querys.
		continue;
	  }

	  for (int needle : config.getIntArray(ConfigNode.BADNAME_GROUPS)) {
		if (client.isInServerGroup(needle)) {
		  // Filter any client that is in a server group that should be ignored.
		  continue;
		}

		int clientId = client.getId();
		final CheckClientName worker = new CheckClientName(client);
		workers.put(clientId, worker);
		executor.schedule(worker, 0, TimeUnit.SECONDS);
	  }
	}
  }

  /** When a client connects to the server, the nickname will be checked for any forbidden words. */
  @Override
  public void onClientJoin(ClientJoinEvent e) {
	final Client client;
	try {
	  client = api.getClientInfo(e.getClientId());
	} catch (TS3CommandFailedException ex) {
	  // Client is a query, do nothing.
	  return;
	}

	final CheckClientName worker = new CheckClientName(client);
	workers.put(client.getId(), worker);
	executor.schedule(worker, 0, TimeUnit.SECONDS);
  }

  @Override
  public void onClientLeave(ClientLeaveEvent e) {
	final CheckClientName worker = workers.remove(e.getClientId());
	if (worker == null) {
	  return;
	}

	worker.shutdown();
  }

  /** When a channel is edited, the new channel's name will be checked for any forbidden words. */
  @Override
  public void onChannelEdit(ChannelEditedEvent e) {
	checkChannel(e.getChannelId());
  }

  /** When a new channel is created, the channel's name will be checked for any forbidden words. */
  @Override
  public void onChannelCreate(ChannelCreateEvent e) {
	checkChannel(e.getChannelId());
  }

  /**
   * Checks whether the channel matches the RegEx pattern. If so, the channel will either be renamed or deleted.
   *
   * @param channelId
   * 		The channel to be checked
   */
  private void checkChannel(int channelId) {
	ChannelInfo channelInfo = api.getChannelInfo(channelId);
	String channelName = channelInfo.getName();

	for (String pattern : config.getStringArray(ConfigNode.BADNAME_PATTERN)) {
	  if (!channelName.matches(pattern)) {
		// Filter channel that do not match the pattern.
		continue;
	  }

	  String channelAction = config.get(ConfigNode.BADNAME_CHANNEL_ACTION);
	  if (channelAction.equals("rename")) {
		try {
		  SimpleDateFormat formattedDate = new SimpleDateFormat();
		  formattedDate.applyPattern("EE', 'dd. MMMM yyyy hh:mm");

		  final String newChannelName = config.get(ConfigNode.BADNAME_RENAME).replaceAll("%date%",
				  formattedDate.format(new Date())).replaceAll(" ", "\u0020");
		  // Edit the channel to the specified name and add a timestamp to prevent other channels from
		  // being deleted if one channel already has the censored name.
		  api.editChannel(channelId, ChannelProperty.CHANNEL_NAME, newChannelName);
		} catch (TS3CommandFailedException ex) {
		  api.deleteChannel(channelId);
		}
	  } else if (channelAction.equals("delete")) {
		try {
		  api.deleteChannel(channelId);
		} catch (TS3CommandFailedException ex) {
		  ex.printStackTrace();
		}
	  }
	}
  }

  /** Checks whether the client's nickname contains any forbidden words. */
  private class CheckClientName implements Runnable {

	private boolean cancelled;
	private final Client client;
	private boolean isWarned;

	private CheckClientName(Client client) {
	  this.client = client;
	}

	@Override
	public void run() {
	  if (cancelled) {
		return;
	  }

	  for (String pattern : config.getStringArray(ConfigNode.BADNAME_PATTERN)) {
		if (!client.getNickname().matches(pattern)) {
		  // Filter clients that do not match the pattern.
		  continue;
		}

		switch (config.get(ConfigNode.BADNAME_CLIENT_ACTION)) {
		  case "warn":
			if (isWarned) {
			  break;
			}
			api.pokeClient(client.getId(), config.get(ConfigNode.BADNAME_CLIENT_MESSAGE));
			isWarned = true;
			break;
		  case "kick":
			api.kickClientFromServer(config.get(ConfigNode.BADNAME_CLIENT_KICK_MESSAGE), client);
			break;
		}
	  }
	  executor.schedule(this, (config.getBoolean(ConfigNode.BOT_SLOWMODE) ? 5 : 1), TimeUnit.SECONDS);
	}

	public void shutdown() {
	  cancelled = true;
	}
  }
}
