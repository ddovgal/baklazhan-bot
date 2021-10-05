package ua.ddovgal.baklazhan.audio;

import java.io.Closeable;
import java.util.Collection;
import java.util.Optional;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import lombok.Data;

import net.dv8tion.jda.api.entities.VoiceChannel;

public interface GuildPlaybackManager extends Closeable {

    PlayActionResult play(String identifier, VoiceChannel voiceChannel);
    PlaybackChangeResult pause();
    PlaybackChangeResult resume();
    Optional<AudioTrack> playNext();
    Collection<AudioTrack> clearQueue();
    Collection<AudioTrack> getQueue();

    @Data
    class PlayActionResult {

        private final AudioTrack resolvedTrack;
        private final AudioPlaylist resolvedPlaylist;
        private final boolean startedImmediately;
        private final Exception occurredException;

        public static PlayActionResult ofTrack(AudioTrack resolvedTrack, boolean startedImmediately) {
            return new PlayActionResult(resolvedTrack, null, startedImmediately, null);
        }

        public static PlayActionResult ofPlaylist(AudioPlaylist resolvedPlaylist, boolean startedImmediately) {
            return new PlayActionResult(null, resolvedPlaylist, startedImmediately, null);
        }

        public static PlayActionResult ofException(Exception exception) {
            return new PlayActionResult(null, null, false, exception);
        }

        public static PlayActionResult ofNothing() {
            return new PlayActionResult(null, null, false, null);
        }
    }

    enum PlaybackChangeResult {
        DONE,
        NOT_DONE,
        NOTHING_TO_CHANGE
    }
}
