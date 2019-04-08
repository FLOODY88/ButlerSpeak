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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manages idling clients.
 */
public class AfkManager extends TS3EventAdapter {

  private final TS3Api api;
  private final Configuration config;
  private final ScheduledExecutorService executor;
  private final Map<Integer, AfkMover> workers;

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

	for (Client client : api.getClients()) {
	  int[] bypassGroups = config.getIntArray(ConfigNode.AFK_GROUPS_BYPASS);
	  for (int group : bypassGroups) {
		if (client.isInServerGroup(group) || client.isServerQueryClient()) {
		  continue;
		}

		final int clientId = client.getId();
		final AfkMover worker = new AfkMover(clientId);
		workers.put(clientId, worker);
		executor.schedule(worker, 0, TimeUnit.SECONDS);
	  }
	}
  }

  @Override
  public void onClientJoin(ClientJoinEvent e) {
	final int clientId = e.getClientId();
	final Client client;
	try {
	  client = api.getClientInfo(clientId);
	} catch (TS3CommandFailedException ex) {
	  // Client is a query, do nothing.
	  return;
	}

	final int[] configGroups = config.getIntArray(ConfigNode.AFK_GROUPS_BYPASS);
	// If and only if certain groups should be bypassed, check whether the client is in any of
	// these groups.
	if (configGroups[0] != -1) {
	  for (int configGroup : configGroups) {
		// If the client is in a group that bypasses, do nothing.
		if (client.isInServerGroup(configGroup)) {
		  return;
		}
	  }
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
	System.out.println("shutdown on leave");
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

	/** Initializes a new instance for the given <code>clientId</code>. */
	private AfkMover(int clientId) {
	  this.clientId = clientId;
	}

	@Override
	public void run() {
	  if (cancelled) {
		// Client disconnected, cancelling current task.
		return;
	  }

	  final Client client = api.getClientInfo(clientId);
	  int[] bypassChannel = config.getIntArray(ConfigNode.AFK_CHANNEL_BYPASS);
	  if (bypassChannel[0] != -1) {
		for (int channel : bypassChannel) {
		  if (client.getChannelId() == channel) {
			// If the client's current channel is one that should be ignored, reschedule and do nothing.
			reschedule();
			return;
		  }
		}
	  }

	  long idleTime = client.getIdleTime() / 1000;
	  final int configIdleTime = config.getInt(ConfigNode.AFK_IDLE_TIME);
	  final int afkChannelId = config.getInt(ConfigNode.AFK_CHANNEL);

	  // Checks whether the client exceeded the idle time.
	  if (idleTime > configIdleTime && !isIdle) {
		// If the client should be notified, he will be either poked or sent a private message.
		if (config.getBoolean(ConfigNode.AFK_NOTIFY)) {
		  final String notifyMessage = config.get(ConfigNode.AFK_NOTIFY_MESSAGE);
		  if (config.get(ConfigNode.AFK_NOTIFY_TYPE).equals("poke")) {
			api.pokeClient(clientId, notifyMessage);
		  } else {
			api.sendPrivateMessage(clientId, notifyMessage);
		  }
		}

		// Finally, move the client to the specified channel and mark as idle.
		api.moveClient(clientId, afkChannelId);
		isIdle = true;
	  } else if (idleTime < configIdleTime && isIdle) {
		// The client is no longer idle.
		isIdle = false;
	  }

	  // If the client exceeded the idle time before being kicked, the client will be removed
	  // from the server.
	  if (config.getBoolean(ConfigNode.AFK_KICK) && idleTime > config.getInt(ConfigNode.AFK_KICK_TIME) && isIdle) {
		api.kickClientFromServer(config.get(ConfigNode.AFK_KICK_REASON), clientId);
	  }

	  reschedule();
	}

	/** Cancels the {@link AfkManager#executor}. */
	private void shutdown() {
	  cancelled = true;
	  isIdle = false;
	}

	/** Reschedules {@link AfkManager#executor}. */
	private void reschedule() {
	  executor.schedule(this, (config.getBoolean(ConfigNode.BOT_SLOWMODE) ? 5 : 1), TimeUnit.SECONDS);
	}
  }
}
