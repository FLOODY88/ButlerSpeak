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
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import me.floody.butlerspeak.ButlerSpeak;
import me.floody.butlerspeak.config.ConfigNode;
import me.floody.butlerspeak.config.Configuration;
import me.floody.butlerspeak.utils.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Checks whether a client is recording.
 */
public class RecordingChecker extends TS3EventAdapter {

  private final TS3Api api;
  private final Configuration config;
  private final ScheduledExecutorService executor;
  private final Map<Integer, RecordingManager> workers;
  private final Log logger;

  /** Simply constructs a new instance. */
  public RecordingChecker(ButlerSpeak plugin) {
	this.api = plugin.getApi();
	this.config = plugin.getConfig();
	this.executor = new ScheduledThreadPoolExecutor(1);
	this.workers = new HashMap<>();
	this.logger = plugin.getAndSetLogger(this.getClass().getName());

	for (Client client : api.getClients()) {
	  int[] bypassGroups = config.getIntArray(ConfigNode.RECORDING_GROUPS);
	  final int clientId = client.getId();
	  final RecordingManager worker = new RecordingManager(clientId);

	  workers.put(clientId, worker);
	  executor.schedule(worker, 0, TimeUnit.SECONDS);
	}
  }

  @Override
  public void onClientJoin(ClientJoinEvent e) {
	final int clientId = e.getClientId();
	final RecordingManager worker = new RecordingManager(clientId);

	workers.put(clientId, worker);
	executor.schedule(worker, 0, TimeUnit.SECONDS);
  }

  @Override
  public void onClientLeave(ClientLeaveEvent e) {
	final RecordingManager worker = workers.remove(e.getClientId());
	if (worker == null) {
	  return;
	}

	worker.shutdown();
  }

  /** Manages recording clients and performs the specified action. */
  private class RecordingManager implements Runnable {

	private boolean cancelled;
	private final int clientId;
	private Client client;

	/** Constructs a new instance for the given clientId. */
	private RecordingManager(int clientId) {
	  this.clientId = clientId;
	}

	@Override
	public void run() {
	  if (cancelled) {
		this.shutdown();
		return;
	  }

	  client = api.getClientInfo(clientId);
	  final int[] bypassGroups = config.getIntArray(ConfigNode.RECORDING_GROUPS);
	  for (int group : bypassGroups) {
		if (client.isInServerGroup(group)) {
		  cancelled = true;
		  return;
		}
	  }

	  if (!client.isRecording()) {
		// When the client is not recording, reschedule the task and do nothing.
		reschedule();
		return;
	  }

	  int[] ignoredChannel = config.getIntArray(ConfigNode.RECORDING_CHANNEL);
	  for (int channel : ignoredChannel) {
		if (client.getChannelId() == channel) {
		  // When the client is in a channel that should be ignored, reschedule the task and do nothing
		  reschedule();
		  return;
		}
	  }

	  // Based on the action, the recording client will either be kicked or moved to the default channel.
	  switch (config.get(ConfigNode.RECORDING_ACTION)) {
		case "kick":
		  api.kickClientFromServer(config.get(ConfigNode.RECORDING_KICK_MSG), client);
		  logger.info("Kicked client " + client.getNickname() + " for recording in a forbidden channel.");
		  break;
		case "move":
		  api.kickClientFromChannel(client);
		  api.sendPrivateMessage(clientId, config.get(ConfigNode.RECORDING_MOVE_MSG));
		  break;
	  }

	  reschedule();
	}

	/** Cancels the {@link RecordingChecker#executor}. This method is called from {@link RecordingChecker#onClientLeave} */
	private void shutdown() {
	  cancelled = true;
	}

	/** Reschedules the {@link RecordingChecker#executor}. */
	private void reschedule() {
	  executor.schedule(this, (config.getBoolean(ConfigNode.BOT_SLOWMODE) ? 5 : 1), TimeUnit.SECONDS);
	}
  }
}
