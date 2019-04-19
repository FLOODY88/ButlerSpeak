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
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import me.floody.butlerspeak.ButlerSpeak;
import me.floody.butlerspeak.config.ConfigNode;
import me.floody.butlerspeak.config.Configuration;
import me.floody.butlerspeak.utils.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Manages idling clients.
 */
public class AfkManager extends TS3EventAdapter {

  private final TS3Api api;
  private final Configuration config;
  private final ScheduledExecutorService executor;
  private final Map<Integer, AfkMover> workers;
  private final Log logger;
  private final List<Integer> ignoredGroups;
  private final List<Integer> ignoredChannels;

  /**
   * Constructs a new instance.
   * <p>
   * Adds two listener for a client join and leave event. When a client's connecting to the server, a new
   * instance of {@link AfkMover AfkMover} will be created which keeps track of the client's idle time and moves the
   * client once exceeding a certain amount of seconds.
   * </p>
   */
  public AfkManager(ButlerSpeak plugin) {
	this.api = plugin.getApi();
	this.config = plugin.getConfig();
	this.executor = new ScheduledThreadPoolExecutor(1);
	this.workers = new HashMap<>();
	this.logger = plugin.getAndSetLogger(this.getClass().getName());
	this.ignoredGroups = config.getIntegerList(ConfigNode.AFK_GROUPS_BYPASS);
	this.ignoredChannels = config.getIntegerList(ConfigNode.AFK_CHANNEL_BYPASS);

	api.getClients().forEach(client -> {
	  if (IntStream.of(client.getServerGroups()).anyMatch(ignoredGroups::contains)) {
		return;
	  }

	  int clientId = client.getId();
	  final AfkMover worker = new AfkMover(clientId);
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
	final int clientId = e.getClientId();
	final Client client;
	try {
	  client = api.getClientInfo(clientId);
	} catch (TS3CommandFailedException ex) {
	  return;
	}

	if (client.isServerQueryClient() || IntStream.of(client.getServerGroups()).anyMatch(ignoredGroups::contains)) {
	  return;
	}

	final AfkMover worker = new AfkMover(clientId);
	workers.put(clientId, worker);
	executor.schedule(worker, 0, TimeUnit.SECONDS);
  }

  @Override
  public void onClientLeave(ClientLeaveEvent e) {
	final AfkMover worker = workers.remove(e.getClientId());
	if (worker == null) {
	  return;
	}

	worker.shutdown();
  }

  /**
   * Keeps track of a client's idle time and moves/kicks the client after exceeding a certain amount of seconds
   * specified in <code>ButlerSpeak.properties</code>.
   */
  private class AfkMover implements Runnable {

	private final int clientId;
	private boolean cancelled;
	private boolean isIdle;

	/**
	 * Initializes a new instance for the given <code>clientId</code>.
	 */
	private AfkMover(int clientId) {
	  this.clientId = clientId;
	}

	@Override
	public void run() {
	  if (cancelled) {
		return;
	  }

	  final Client client = api.getClientInfo(clientId);
	  if (ignoredChannels.contains(client.getChannelId())) {
		reschedule();
		return;
	  }

	  long idleTime = client.getIdleTime() / 1000;
	  long configIdleTime = config.getLong(ConfigNode.AFK_IDLE_TIME);
	  if (idleTime > configIdleTime && !isIdle) {
		if (config.getBoolean(ConfigNode.AFK_NOTIFY)) {
		  String notifyMessage = config.get(ConfigNode.AFK_NOTIFY_MESSAGE);
		  switch (config.get(ConfigNode.AFK_NOTIFY_TYPE)) {
			case "poke":
			  api.pokeClient(clientId, notifyMessage);
			  break;
			case "chat":
			  api.sendPrivateMessage(clientId, notifyMessage);
			  break;
		  }
		}

		this.isIdle = true;
		api.moveClient(clientId, config.getInt(ConfigNode.AFK_CHANNEL));
	  } else if (idleTime < configIdleTime && isIdle) {
		this.isIdle = false;
	  }

	  if (config.getBoolean(ConfigNode.AFK_KICK) && (idleTime > config.getLong(ConfigNode.AFK_KICK_TIME) && isIdle)) {
		api.kickClientFromServer(config.get(ConfigNode.AFK_KICK_REASON), clientId);
		logger.info("Kicked client " + client.getNickname() + "( " + clientId + ") for being idle too long!");
	  }

	  reschedule();
	}

	/**
	 * Cancels the {@link AfkManager#executor}.
	 */
	private void shutdown() {
	  cancelled = true;
	  isIdle = false;
	}

	/**
	 * Reschedules {@link AfkManager#executor}.
	 */
	private void reschedule() {
	  executor.schedule(this, (config.getBoolean(ConfigNode.BOT_SLOWMODE) ? 5 : 1), TimeUnit.SECONDS);
	}
  }
}
