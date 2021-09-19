package ua.ddovgal.baklazhan;

import java.util.List;
import java.util.Queue;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import lombok.Getter;

public class TrackScheduler implements AudioLoadResultHandler {

    private final AudioPlayer player;
    @Getter
    private final Queue<AudioTrack> queue;

    public TrackScheduler(AudioPlayer player, Queue<AudioTrack> queue) {
        this.player = player;
        this.queue = queue;

        player.addListener(event -> {
            if (event instanceof TrackEndEvent) {
                TrackEndEvent trackEndEvent = (TrackEndEvent) event;
                if (trackEndEvent.endReason == AudioTrackEndReason.FINISHED) {
                    playNext();
                }
            }
        });
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        // LavaPlayer found an audio source for us to play
        if (!queue.isEmpty()) {
            queue.offer(track);
        } else {
            player.playTrack(track);
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        // LavaPlayer found multiple AudioTracks from some playlist
        List<AudioTrack> tracks = playlist.getTracks();

        if (!queue.isEmpty()) {
            queue.addAll(tracks);
        } else {
            AudioTrack track = tracks.remove(0);
            queue.addAll(tracks);
            player.playTrack(track);
        }
    }

    @Override
    public void noMatches() {
        // LavaPlayer did not find any audio to extract
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        // LavaPlayer could not parse an audio source for some reason
    }

    public boolean playNext() {
        AudioTrack nextTrack = queue.poll();
        if (nextTrack != null) {
            player.setPaused(false);
            player.playTrack(nextTrack);
            return true;
        }
        return false;
    }
}
