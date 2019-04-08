## ButlerSpeak

![Total Downloads](https://img.shields.io/github/downloads/FLOODY88/ButlerSpeak/total.svg) [![Issues Open](https://img.shields.io/github/issues/FLOODY88/ButlerSpeak.svg)](../../issues) [![Latest Release](https://img.shields.io/github/release/FLOODY88/ButlerSpeak.svg)](../../releases) 

An open-source TeamSpeak 3 server query bot written in Java.

## Features

1. **Advertisement**: Broadcasts a message to the whole server every ``X`` minutes.
2. **Afk Manager**: Moves a client to a specific channel when being idle for more than ``X`` minutes.
3. **Name Checker**: 
   - Scans existing and new channels for forbidding words. On a match, the coressponding channel will either be 
     deleted or renamed.
   - Scans all clients for forbidden words in their names. On a match, the corresponding client will either be 
     kicked or warned.
4. **Recording Checker**: Searches for clients that are recording in forbidden channels. The corresponding client will
   either be kicked from the server or the server (and warned).
5. **Support**: When a client enters a certain channel, certain groups will be notified that a client requested help.
   - Optional: A sub-channel of the support-channel can be created once the client enteres the specific channel
6. **Welcome-Message**: Sends a message to certain groups when connecting to the server.

All features can be disabled by editing the corresponding configuration file, for more information please see [here](#butlerspeak-configuration).



## Getting Started

#### Download

Download the [latest release](../../releases/latest) and upload the `jar` to your server.

#### Configuration

Please ensure that the configuration file (`ButlerSpeak.properties`) is in the same directory as the `jar` and adjust the values to successfully connect to the server.

Please see [here](#butlerspeak-configuration) for further reference.

#### Usage

Before starting the `jar`, please ensure that Java is installed. You may do this by using `java -version`.

- **Starting the bot in the foreground**:  `java -jar ButlerSpeak.jar`
- **Starting the bot in the background**:  `java -jar ButlerSpeak.jar &`
- **Starting the bot with limited RAM usage**: `java -mx30M -jar ButlerSpeak.jar`

Note that the file name may be different and that there are also other ways to start the `jar`.

## ButlerSpeak Configuration

This bot is highly customizable by editing the corresponding configuration file. The following example illustrates how to set up the connection information and the query login credentials.

**Example**:

```properties
# The host to connect to.
server.host=127.0.0.1
# The server's port.
server.port=9987
# The query's login credentials.
query.username=serveradmin
query.password=hackablepassword123
# The query port to connect to.
query.port=10011
```

For a full example of the configuration file, please see [here](../master/src/main/resources/ButlerSpeak_EXAMPLE.properties). The file should be in the same directory as the `jar` and needs to be called `ButlerSpeak.properties`, otherwise the bot cannot read your configuration file and will shut down. 
When you start the bot without a configuration file, the example will be copied and the bot shuts down. You need to adjust the file and restart the bot afterwards.

Note that you don't need the full configuration file when you're not using certain plugins. Thus, not using the AfkManager won't require you to add any `afk` property. 

The following example illustrates how all features can be enabled:

```properties
# Defines which features should be enabled.
# 	welcome 		- The welcome messages
#	badname 		- The name checker for forbidden words
#	recording 		- The recording checker
#	support 		- The support handler
# 	advertisement 	        - The advertisement
# 	afk 			- The afk manager
bot.plugins=welcome, badname, recording, support, advertisement, afk
```



## Questions, bugs or enhancements?

All features are tested multiple times on bigger servers, but you can still run into some issues. Also, please get in touch with me if you have any questions for ehancements for further releases.

When any of the above matches your current situation, please let me know [here](../../issues). I'll try to help you as soon as possible.
