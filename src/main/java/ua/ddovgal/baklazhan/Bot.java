package ua.ddovgal.baklazhan;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.slf4j.Log4jLogger;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import lombok.extern.slf4j.Slf4j;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;

@Slf4j
public class Bot {

    private static final String TOKEN = System.getenv("DISCORD_TOKEN");

    public static void main(String[] args) {
        if (TOKEN == null) {
            throw new IllegalStateException("DISCORD_TOKEN envvar isn't found");
        }

        final DiscordClient client = DiscordClient.create(TOKEN);
        final GatewayDiscordClient gateway = client.login().block();
        log.info("Started");

        // Creates AudioPlayer instances and translates URLs to AudioTrack instances
        final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

        // This is an optimization strategy that Discord4J can utilize.
        // It is not important to understand
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

        // Allow playerManager to parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);

        // Create an AudioPlayer so Discord4J can receive audio data
        final AudioPlayer player = playerManager.createPlayer();

        // We will be creating LavaPlayerAudioProvider in the next step
        AudioProvider provider = new LavaPlayerAudioProvider(player);
        TrackScheduler scheduler = new TrackScheduler(player);

        gateway.on(MessageCreateEvent.class).subscribe(event -> {
            final Message message = event.getMessage();
            log.info("Message={}", message);
            if ("Ora ora".equals(message.getContent())) {
                final MessageChannel channel = message.getChannel().block();
                channel.createMessage("Muda muda").block();
            } else if ("join".equals(message.getContent())) {
                Snowflake userId = message.getAuthor().get().getId();
                VoiceChannel voiceChannel = event
                    .getGuild()
                    .block()
                    .getChannels()
                    .filter(VoiceChannel.class::isInstance)
                    .map(VoiceChannel.class::cast)
                    .filterWhen(channel -> channel.isMemberConnected(userId))
                    .blockFirst();
                voiceChannel.join(voiceChannelJoinSpec -> voiceChannelJoinSpec.setProvider(provider)).block();
            } else if (message.getContent().startsWith("play")) {
                String content = message.getContent();
                List<String> command = Arrays.asList(content.split(" "));
                playerManager.loadItem(command.get(1), scheduler);
            }
        });

        gateway.onDisconnect().block();
    }
}
