package ua.ddovgal.baklazhan.handler.impl;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import lombok.RequiredArgsConstructor;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ua.ddovgal.baklazhan.audio.GuildPlaybackManager;
import ua.ddovgal.baklazhan.audio.GuildPlaybackManager.PlaybackChangeResult;
import ua.ddovgal.baklazhan.audio.GuildPlaybackManagersCurator;
import ua.ddovgal.baklazhan.handler.EventHandler;

@RequiredArgsConstructor
public class PlaybackControlsHandler implements EventHandler<GuildMessageReceivedEvent> {

    private final GuildPlaybackManagersCurator guildPlaybackManagersCurator;

    private final Map<String, BiConsumer<Message, GuildPlaybackManager>> handlers = Map.of(
        "!pause", this::pause,
        "!resume", this::resume,
        "!next", this::next,
        "!queue", this::queue,
        "!clear", this::clear,
        "!bye", this::bye
    );

    @Override
    public void handle(GuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        BiConsumer<Message, GuildPlaybackManager> handlerMethod = handlers.get(message.getContentRaw());
        if (handlerMethod != null) {
            GuildPlaybackManager guildPlaybackManager = guildPlaybackManagersCurator.getPlaybackManager(event.getGuild());
            guildPlaybackManagersCurator.setInformingChannel(event.getGuild(), message.getTextChannel());
            handlerMethod.accept(message, guildPlaybackManager);
        }
    }

    private void pause(Message message, GuildPlaybackManager guildPlaybackManager) {
        PlaybackChangeResult changeResult = guildPlaybackManager.pause();

        String replyMessageText;
        if (changeResult == PlaybackChangeResult.DONE) {
            replyMessageText = "Paused";
        } else if (changeResult == PlaybackChangeResult.NOT_DONE) {
            replyMessageText = "Was already paused";
        } else if (changeResult == PlaybackChangeResult.NOTHING_TO_CHANGE) {
            replyMessageText = "Nothing was playing to pause";
        } else {
            throw new RuntimeException("Unexpected value of PlaybackChangeResult: " + changeResult.name());
        }

        message.reply(replyMessageText).queue();
    }

    private void resume(Message message, GuildPlaybackManager guildPlaybackManager) {
        PlaybackChangeResult changeResult = guildPlaybackManager.resume();

        String replyMessageText;
        if (changeResult == PlaybackChangeResult.DONE) {
            replyMessageText = "Resumed";
        } else if (changeResult == PlaybackChangeResult.NOT_DONE) {
            replyMessageText = "Was already playing";
        } else if (changeResult == PlaybackChangeResult.NOTHING_TO_CHANGE) {
            replyMessageText = "Nothing was playing to resume";
        } else {
            throw new RuntimeException("Unexpected value of PlaybackChangeResult: " + changeResult.name());
        }

        message.reply(replyMessageText).queue();
    }

    private void next(Message message, GuildPlaybackManager guildPlaybackManager) {
        String replyMessageText = guildPlaybackManager
            .playNext()
            .map(track -> "Playing `" + track.getInfo().title + "`")
            .orElse("Nothing to play next");

        message.reply(replyMessageText).queue();
    }

    private void queue(Message message, GuildPlaybackManager guildPlaybackManager) {
        String currentTracks = guildPlaybackManager
            .getQueue()
            .stream()
            .map(AudioTrack::getInfo)
            .map(info -> " - `" + info.title + "`")
            .collect(Collectors.joining("\n"));

        String replyMessageText;
        if (currentTracks.isEmpty()) {
            replyMessageText = "The queue is empty";
        } else {
            replyMessageText = "**Queued tracks**:\n\n" + currentTracks;
        }

        message.reply(replyMessageText).queue();
    }

    private void clear(Message message, GuildPlaybackManager guildPlaybackManager) {
        String cleanedTracks = guildPlaybackManager
            .clearQueue()
            .stream()
            .map(AudioTrack::getInfo)
            .map(info -> " - `" + info.title + "`")
            .collect(Collectors.joining("\n"));

        String cleanedTracksMessage;
        if (cleanedTracks.isEmpty()) {
            cleanedTracksMessage = "The queue was already empty";
        } else {
            cleanedTracksMessage = "**Cleaned queue of the following tracks**:\n\n" + cleanedTracks;
        }

        message.reply(cleanedTracksMessage).queue();
    }

    private void bye(Message message, GuildPlaybackManager guildPlaybackManager) {
        guildPlaybackManagersCurator.closePlaybackManager(message.getGuild().getIdLong());
        message.reply("See ya later").queue();
    }
}
