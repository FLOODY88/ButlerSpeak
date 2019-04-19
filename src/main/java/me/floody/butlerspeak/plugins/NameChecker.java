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
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import me.floody.butlerspeak.ButlerSpeak;
import me.floody.butlerspeak.config.ConfigNode;
import me.floody.butlerspeak.config.Configuration;
import me.floody.butlerspeak.utils.Log;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Checks channel and client's name for forbidden words.
 */
public class NameChecker extends TS3EventAdapter {

  private final TS3Api api;
  private final Configuration config;
  private final ScheduledExecutorService executor;
  private final Map<Integer, CheckClientName> workers;
  private final Log logger;
  private final List<String> regexPattern;
  private final List<Integer> ignoredChannel;
  private final List<Integer> ignoredGroups;

  /**
   * Initializes a new instance.
   * <p>
   * When first initialized, all clients and channels will be if they contain any forbidden words that matches the
   * specified regexPattern. Also, it listens to {@link ClientJoinEvent}, {@link ChannelEditedEvent} and
   * {@link ChannelCreateEvent} to check the name upon changes.
   * </p>
   */
  public NameChecker(ButlerSpeak plugin) {
	this.api = plugin.getApi();
	this.config = plugin.getConfig();
	this.executor = new ScheduledThreadPoolExecutor(1);
	this.workers = new HashMap<>();
	this.logger = plugin.getAndSetLogger(this.getClass().getName());
	this.regexPattern = config.getStringList(ConfigNode.BADNAME_PATTERN);
	this.ignoredChannel = config.getIntegerList(ConfigNode.BADNAME_CHANNEL);
	this.ignoredGroups = config.getIntegerList(ConfigNode.BADNAME_GROUPS);

	// On first start, check the existing channels and connected clients for bad names.
	api.getChannels().forEach(channel -> checkChannel(channel.getId()));
	api.getClients().forEach(client -> {
	  if (Arrays.stream(client.getServerGroups()).anyMatch((ignoredGroups::contains))) {
		return;
	  }

	  final CheckClientName worker = new CheckClientName(client);
	  workers.put(client.getId(), worker);
	  executor.schedule(worker, 0, TimeUnit.SECONDS);

	  // After scheduling the task for a client, wait a bit to prevent flooding.
	  try {
		Thread.sleep(350);
	  } catch (InterruptedException ex) {
		// Nothing
	  }
	});
  }

  @Override
  public void onClientJoin(ClientJoinEvent e) {
	final Client client;
	try {
	  client = api.getClientInfo(e.getClientId());
	} catch (TS3CommandFailedException ex) {
	  // Client is a query, do nothing.
	  return;
	}

	if (client.isServerQueryClient() || IntStream.of(client.getServerGroups()).anyMatch(ignoredGroups::contains)) {
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
   * Checks whether the channel matches the RegEx regexPattern. If so, the channel will either be renamed or deleted.
   *
   * @param channelId
   * 		The channel to be checked
   */
  private void checkChannel(int channelId) {
	if (ignoredChannel.contains(channelId)) {
	  return;
	}

	ChannelInfo channelInfo = api.getChannelInfo(channelId);
	String channelName = channelInfo.getName();
	regexPattern.forEach(pattern -> {
	  Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
	  if (!p.matcher(channelName).matches()) {
		return;
	  }

	  switch (config.get(ConfigNode.BADNAME_CHANNEL_ACTION)) {
		case "rename":
		  SimpleDateFormat simpleDate = new SimpleDateFormat();
		  simpleDate.applyPattern("dd-MMM, HH:mm");

		  String newChannelName = config.get(ConfigNode.BADNAME_RENAME)
				  .replaceAll("%date%", simpleDate.format(new Date()))
				  .replaceAll("\\s+", "\u0020");

		  try {
			api.editChannel(channelId, ChannelProperty.CHANNEL_NAME, newChannelName);
		  } catch (TS3CommandFailedException ex) {
			api.deleteChannel(channelId);
		  }
		  break;
		case "delete":
		  try {
			api.deleteChannel(channelId);
		  } catch (TS3CommandFailedException ex) {
			logger.error("Could not delete channel: " + channelName, ex);
		  }
		  break;
	  }
	});

	// Needs to wait a moment to prevent flooding.
	try {
	  Thread.sleep(350);
	} catch (InterruptedException ex) {
	  // Nothing
	}
  }

  /**
   * Checks whether the client's nickname contains any forbidden words.
   */
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

	  String clientName = client.getNickname();
	  regexPattern.forEach(pattern -> {
		Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		if (!p.matcher(clientName).matches()) {
		  return;
		}

		switch (config.get(ConfigNode.BADNAME_CLIENT_ACTION)) {
		  case "warn":
			if (isWarned) {
			  break;
			}

			isWarned = true;
			api.pokeClient(client.getId(), config.get(ConfigNode.BADNAME_CLIENT_MESSAGE));
			break;
		  case "kick":
			api.kickClientFromServer(config.get(ConfigNode.BADNAME_CLIENT_KICK_MESSAGE), client);
			break;
		}
	  });

	  executor.schedule(this, (config.getBoolean(ConfigNode.BOT_SLOWMODE) ? 5 : 1), TimeUnit.SECONDS);
	}

	public void shutdown() {
	  cancelled = true;
	}
  }
}
