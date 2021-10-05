package ua.ddovgal.baklazhan;

import java.util.Collection;
import java.util.List;

import javax.security.auth.login.LoginException;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;

import lombok.extern.slf4j.Slf4j;

import ua.ddovgal.baklazhan.audio.DefaultGuildPlaybackManagersCurator;
import ua.ddovgal.baklazhan.audio.GuildPlaybackManagersCurator;
import ua.ddovgal.baklazhan.handler.EventHandler;
import ua.ddovgal.baklazhan.handler.impl.HelpEventHandler;
import ua.ddovgal.baklazhan.handler.impl.PingPongEventHandler;
import ua.ddovgal.baklazhan.handler.impl.PlaySomethingEventHandler;
import ua.ddovgal.baklazhan.handler.impl.PlaybackControlsHandler;

@Slf4j
public class BotRunner {

    public static void main(String[] args) throws Exception {

        // ------ DI by hands
        String botToken = getBotToken();
        AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
        Iterable<AudioSourceManager> additionalManagers = List.of();
        GuildPlaybackManagersCurator guildPlaybackManagersCurator
            = new DefaultGuildPlaybackManagersCurator(audioPlayerManager, additionalManagers);
        Collection<EventHandler<?>> eventHandlers = List.of(
            new PingPongEventHandler(),
            new PlaySomethingEventHandler(guildPlaybackManagersCurator),
            new HelpEventHandler(),
            new PlaybackControlsHandler(guildPlaybackManagersCurator)
        );
        // ------

        BaklazhanBot baklazhanBot = new BaklazhanBot(botToken, eventHandlers);
        try {
            log.info("Bot is starting...");
            baklazhanBot.buildAndStart().awaitReady();
            log.info("Bot is started");
        } catch (LoginException e) {
            log.error("Failed to login", e);
            throw e;
        } catch (InterruptedException e) {
            log.error("JDA hasn't reached CONNECTED status", e);
            throw e;
        }
    }

    private static String getBotToken() {
        String botToken = System.getenv("DISCORD_TOKEN");
        if (botToken == null) {
            String message = "There is no DISCORD_TOKEN envvar specified";
            log.error(message);
            throw new IllegalStateException(message);
        }
        return botToken;
    }
}
