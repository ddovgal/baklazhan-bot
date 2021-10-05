package ua.ddovgal.baklazhan.audio;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class DefaultGuildPlaybackManagersCurator implements GuildPlaybackManagersCurator {

    private final Map<Long, GuildPlaybackManager> guildPlaybackManagers = new ConcurrentHashMap<>();
    private final Map<Long, DefaultAudioEventsInformer> defaultAudioEventsInformers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService abandonedMonitor = Executors.newSingleThreadScheduledExecutor();

    private final AudioPlayerManager audioPlayerManager;

    public DefaultGuildPlaybackManagersCurator(AudioPlayerManager audioPlayerManager, Iterable<AudioSourceManager> additionalManagers) {
        this.audioPlayerManager = audioPlayerManager;

        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
        additionalManagers.forEach(audioPlayerManager::registerSourceManager);
    }

    @Override
    public GuildPlaybackManager getPlaybackManager(Guild guild) {
        long guildId = guild.getIdLong();
        GuildPlaybackManager guildPlaybackManager = guildPlaybackManagers.get(guildId);

        if (guildPlaybackManager == null) {
            DefaultAudioEventsInformer defaultAudioEventsInformer = new DefaultAudioEventsInformer(guildId, guild.getJDA(), this);
            guildPlaybackManager = new DefaultGuildPlaybackManager(guild, audioPlayerManager, defaultAudioEventsInformer, abandonedMonitor);
            guildPlaybackManagers.put(guildId, guildPlaybackManager);
            defaultAudioEventsInformers.put(guildId, defaultAudioEventsInformer);
        }

        return guildPlaybackManager;
    }

    @Override
    public boolean setInformingChannel(Guild guild, TextChannel channel) {
        DefaultAudioEventsInformer defaultAudioEventsInformer = defaultAudioEventsInformers.get(guild.getIdLong());
        if (defaultAudioEventsInformer == null) {
            return false;
        } else {
            defaultAudioEventsInformer.setTextChannelId(channel.getIdLong());
            return true;
        }
    }

    @Override
    public boolean closePlaybackManager(long guildId) {
        GuildPlaybackManager guildPlaybackManager = guildPlaybackManagers.remove(guildId);

        if (guildPlaybackManager == null) {
            return false;
        }

        try {
            defaultAudioEventsInformers.remove(guildId);
            guildPlaybackManager.close();
        } catch (IOException e) {
            throw new RuntimeException("Shouldn't happen", e);
        }

        return true;
    }
}
