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

package me.floody.butlerspeak.config;

/**
 * Contains list of all config nodes used.
 */
public enum ConfigNode {
  BOT_USERNAME("bot.username"),
  BOT_CHANNEL("bot.channel"),
  BOT_SLOWMODE("bot.slowmode"),
  BOT_PLUGINS("bot.plugins"),
  SERVER_HOST("server.host"),
  SERVER_PORT("server.port"),
  QUERY_PORT("query.port"),
  QUERY_USERNAME("query.username"),
  QUERY_PASSWORD("query.password"),

  WELCOME_MESSAGE("welcome.message"),
  WELCOME_CONNECTIONS("welcome.connections"),
  WELCOME_GROUPS("welcome.groups"),

  AFK_IDLE_TIME("afk.idle-time"),
  AFK_CHANNEL("afk.channel"),
  AFK_NOTIFY("afk.notify"),
  AFK_NOTIFY_TYPE("afk.notify-type"),
  AFK_NOTIFY_MESSAGE("afk.notify-message"),
  AFK_KICK("afk.kick"),
  AFK_KICK_TIME("afk.kick-time"),
  AFK_KICK_REASON("afk.kick-reason"),
  AFK_GROUPS_BYPASS("afk.bypass-groups"),
  AFK_CHANNEL_BYPASS("afk.bypass-channel"),

  BADNAME_CHANNEL("badname.bypass-channel"),
  BADNAME_GROUPS("badname.bypass-groups"),
  BADNAME_PATTERN("badname.pattern"),
  BADNAME_CHANNEL_ACTION("badname.channel-action"),
  BADNAME_CLIENT_ACTION("badname.client-action"),
  BADNAME_CLIENT_MESSAGE("badname.client-warn-message"),
  BADNAME_CLIENT_KICK_MESSAGE("badname.client-kick-message"),
  BADNAME_RENAME("badname.channel-rename"),

  RECORDING_ACTION("recording.action"),
  RECORDING_MOVE_MSG("recording.move-message"),
  RECORDING_KICK_MSG("recording.kick-message"),
  RECORDING_CHANNEL("recording.bypass-channel"),
  RECORDING_GROUPS("recording.bypass-groups"),

  SUPPORT_CHANNEL("support.channel"),
  SUPPORT_CREATE_CHANNEL("support.channel-create"),
  SUPPORT_CHANNEL_NAME("support.channel-name"),
  SUPPORT_NOTIFY_GROUPS("support.notify-groups"),
  SUPPORT_NOTIFY_MESSAGE("support.notify-message"),
  SUPPORT_NOTIFY_FAIL("support.message-fail"),
  SUPPORT_MESSAGE("support.message"),

  ADVERTISEMENT_MESSAGE("advertisement.message"),
  ADVERTISEMENT_DELAY("advertisement.delay");

  /** The key of the node. */
  private String key;

  /** Simply sets {@link #key} to the desired value specified by <code>key</code>. */
  ConfigNode(String key) {
    this.key = key;
  }

  /** Returns the key of the desired node. */
  public String getKey() {
    return key;
  }
}
