package ua.ddovgal.baklazhan.audio;

import org.jetbrains.annotations.NotNull;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

@RequiredArgsConstructor
public class DefaultAudioEventsInformer implements AudioEventsInformer {

    private final long guildId;
    private final JDA jda;
    private final GuildPlaybackManagersCurator guildPlaybackManagersCurator;

    @Setter
    private long textChannelId;

    @Override
    public void informPlayingNextTrack(AudioTrack track) {
        getTextChannel().sendMessage("Next track has started `" + track.getInfo().title + "`").queue();
    }

    @Override
    public void informAutoDisconnecting() {
        guildPlaybackManagersCurator.closePlaybackManager(guildId);
        getTextChannel().sendMessage("Meh, nothing to do here, disconnecting by timeout").queue();
    }

    @NotNull
    private TextChannel getTextChannel() {
        TextChannel textChannel = jda.getTextChannelById(textChannelId);
        if (textChannel == null) {
            throw new RuntimeException("textChannelId has become invalid");
        }
        return textChannel;
    }
}
