package ua.ddovgal.baklazhan.handler.impl;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ua.ddovgal.baklazhan.audio.GuildPlaybackManager;
import ua.ddovgal.baklazhan.audio.GuildPlaybackManager.PlayActionResult;
import ua.ddovgal.baklazhan.audio.GuildPlaybackManagersCurator;
import ua.ddovgal.baklazhan.handler.SharingConditionalEventHandler;

import static ua.ddovgal.baklazhan.util.DiscordUtils.getCurrentlyConnected;

@Slf4j
@RequiredArgsConstructor
public class PlaySomethingEventHandler extends SharingConditionalEventHandler<GuildMessageReceivedEvent, String> {

    private final GuildPlaybackManagersCurator guildPlaybackManagersCurator;

    @Override
    public Optional<String> checkSuitability(GuildMessageReceivedEvent event) {
        String messageText = event.getMessage().getContentRaw();
        String[] parts = messageText.split(" ");

        if (parts.length == 2 && parts[0].equals("!play") && StringUtils.isNotBlank(parts[1])) {
            return Optional.of(parts[1]);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void handleSuitable(GuildMessageReceivedEvent event, String identifier) {
        Guild guild = event.getGuild();
        Message message = event.getMessage();

        log.info("Play request; guildName={}; guildId={}; identifier={}", guild.getName(), guild.getIdLong(), identifier);

        Optional<VoiceChannel> optionalVoiceChannel = getCurrentlyConnected(message.getAuthor().getIdLong(), guild.getVoiceChannels());
        if (optionalVoiceChannel.isEmpty()) {
            message.reply("Please, join some voice channel, so I would know where to play").queue();
            return;
        }

        GuildPlaybackManager guildPlaybackManager = guildPlaybackManagersCurator.getPlaybackManager(guild);
        guildPlaybackManagersCurator.setInformingChannel(guild, message.getTextChannel());
        PlayActionResult playActionResult = guildPlaybackManager.play(identifier, optionalVoiceChannel.get());
        AudioTrack track = playActionResult.getResolvedTrack();
        AudioPlaylist playlist = playActionResult.getResolvedPlaylist();
        boolean startedImmediately = playActionResult.isStartedImmediately();
        Exception exception = playActionResult.getOccurredException();

        String responseMessageText;
        if (track != null) {
            responseMessageText = (startedImmediately ? "Playing `" : "Queued `") + track.getInfo().title + "`";
        } else if (playlist != null) {
            if (startedImmediately) {
                String playlistFirstTrackName = playlist.getTracks().get(0).getInfo().title;
                responseMessageText = "Queued `" + playlist.getName() + "`\nPlaying its first track `" + playlistFirstTrackName + "`";
            } else {
                responseMessageText = "Queued `" + playlist.getName() + "`";
            }
        } else if (exception == null) {
            responseMessageText = "Sorry, I don't know how to play `" + identifier + "`";
        } else {
            responseMessageText = "Failed to play `" + identifier + "`\n\n```\n" + exception.getMessage() + "\n```";
        }
        message.reply(responseMessageText).queue();
    }
}
