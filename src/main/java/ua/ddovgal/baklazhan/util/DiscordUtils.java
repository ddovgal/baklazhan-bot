package ua.ddovgal.baklazhan.util;

import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.dv8tion.jda.api.entities.VoiceChannel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DiscordUtils {

    public static Optional<VoiceChannel> getCurrentlyConnected(long userId, List<VoiceChannel> voiceChannels) {
        return voiceChannels
            .stream()
            .filter(voiceChannel -> voiceChannel.getMembers().stream().anyMatch(member -> member.getUser().getIdLong() == userId))
            .findAny();
    }
}
