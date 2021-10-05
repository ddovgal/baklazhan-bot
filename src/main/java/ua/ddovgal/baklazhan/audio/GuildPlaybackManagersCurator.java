package ua.ddovgal.baklazhan.audio;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public interface GuildPlaybackManagersCurator {
    GuildPlaybackManager getPlaybackManager(Guild guild);
    boolean setInformingChannel(Guild guild, TextChannel channel);
    boolean closePlaybackManager(long guildId);
}
