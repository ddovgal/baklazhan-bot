package ua.ddovgal.baklazhan;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;

@Slf4j
public class Bot {

    private static final String TOKEN = System.getenv("DISCORD_TOKEN");

    public static void main(String[] args) {
        if (TOKEN == null) {
            throw new IllegalStateException("DISCORD_TOKEN envvar isn't found");
        }

        final DiscordClient client = DiscordClient.create(TOKEN);
        final GatewayDiscordClient gateway = client.login().block();

        if (gateway == null) {
            throw new IllegalStateException("GatewayDiscordClient is null");
        }
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
        TrackScheduler scheduler = new TrackScheduler(player, new ConcurrentLinkedQueue<>());

        Context context = new Context(playerManager, player, provider, scheduler);
        gateway.on(MessageCreateEvent.class).subscribe(event -> {
            log.info("MessageCreateEvent={}", event);
            for (BiFunction<MessageCreateEvent, Context, Boolean> handler : handlers) {
                Boolean wasSuitable = false;
                try {
                    wasSuitable = handler.apply(event, context);
                } catch (Exception e) {
                    log.error("Uncaught exception", e);
                }
                if (wasSuitable) {
                    break;
                }
            }
        });

        gateway.onDisconnect().block();
    }

    private static final List<BiFunction<MessageCreateEvent, Context, Boolean>> handlers = List.of(
        (event, context) -> {
            Message message = event.getMessage();
            if (!"!join".equals(message.getContent())) {
                return false;
            }

            Snowflake userId = message.getAuthor().get().getId();
            VoiceChannel voiceChannel = event
                .getGuild()
                .block()
                .getChannels()
                .filter(VoiceChannel.class::isInstance)
                .map(VoiceChannel.class::cast)
                .filterWhen(channel -> channel.isMemberConnected(userId))
                .blockFirst();
            voiceChannel.join(voiceChannelJoinSpec -> voiceChannelJoinSpec.setProvider(context.getProvider())).block();

            return true;
        },
        (event, context) -> {
            Message message = event.getMessage();
            String messageContent = message.getContent();
            if (!messageContent.startsWith("!play")) {
                return false;
            }

            context.getPlayerManager().loadItem(messageContent.split(" ")[1], context.getScheduler());

            return true;
        },
        (event, context) -> {
            Message message = event.getMessage();
            if (!"!pause".equals(message.getContent())) {
                return false;
            }

            context.getPlayer().setPaused(true);

            return true;
        },
        (event, context) -> {
            Message message = event.getMessage();
            if (!"!resume".equals(message.getContent())) {
                return false;
            }

            context.getPlayer().setPaused(false);

            return true;
        },
        (event, context) -> {
            Message message = event.getMessage();
            String messageContent = message.getContent();
            if (!messageContent.startsWith("!volume")) {
                return false;
            }

            context.getPlayer().setVolume(Integer.parseInt(messageContent.split(" ")[1]));

            return true;
        },
        (event, context) -> {
            Message message = event.getMessage();
            if (!"!leave".equals(message.getContent())) {
                return false;
            }

            context.getPlayer().stopTrack();
            context.getScheduler().getQueue().clear();
            event.getGuild().flatMap(Guild::getVoiceConnection).flatMap(VoiceConnection::disconnect).block();

            return true;
        },
        (event, context) -> {
            Message message = event.getMessage();
            if (!"!next".equals(message.getContent())) {
                return false;
            }

            boolean hasNext = context.getScheduler().playNext();
            if (!hasNext) {
                MessageChannel channel = message.getChannel().block();
                channel.createMessage("There is no next track").block();
            }

            return true;
        }
    );

    @Value
    private static class Context {
        AudioPlayerManager playerManager;
        AudioPlayer player;
        AudioProvider provider;
        TrackScheduler scheduler;
    }
}
