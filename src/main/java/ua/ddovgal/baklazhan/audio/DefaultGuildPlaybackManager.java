package ua.ddovgal.baklazhan.audio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import ua.ddovgal.baklazhan.util.Reference;

@Slf4j
public class DefaultGuildPlaybackManager implements GuildPlaybackManager {

    private final AudioPlayerManager audioPlayerManager;
    private final AudioPlayer audioPlayer;

    private final AudioManager audioManager;

    private final AudioEventsInformer informer;
    private final ScheduledExecutorService abandonedMonitor;

    private final Queue<AudioTrack> queuedTracks = new ConcurrentLinkedQueue<>();
    private final long AUTO_DISCONNECT_DELAY_MS = TimeUnit.MINUTES.toMillis(5);
    private ScheduledFuture<?> abandonedCheckerTask;

    public DefaultGuildPlaybackManager(Guild guild,
                                       AudioPlayerManager audioPlayerManager,
                                       AudioEventsInformer informer,
                                       ScheduledExecutorService abandonedMonitor) {
        audioManager = guild.getAudioManager();
        audioPlayer = audioPlayerManager.createPlayer();
        this.audioPlayerManager = audioPlayerManager;
        this.informer = informer;
        this.abandonedMonitor = abandonedMonitor;

        audioManager.setSendingHandler(new AudioPlayerBackedAudioSendHandler(audioPlayer));
        audioManager.setSelfDeafened(true); // we don't need to listen
        audioPlayer.addListener(new PlayNextTrackAudioEventListener());
        audioPlayer.addListener(new AutoDisconnectAudioEventListener());
    }

    @Override
    public PlayActionResult play(String identifier, VoiceChannel voiceChannel) {
        ensureConnectedTo(voiceChannel);

        Reference<PlayActionResult> resultReference = Reference.nothing();
        try {
            audioPlayerManager
                .loadItemOrdered(this, identifier, new DefaultAudioLoadResultHandler(resultReference))
                .get(); // wait until completed and handler will set result into the reference
        } catch (InterruptedException | ExecutionException e) {
            resultReference.setObject(PlayActionResult.ofException(e));
        }

        PlayActionResult result = resultReference.getObject();
        if (result.getOccurredException() != null) {
            log.error("Failed to loadItemOrdered; identifier={}", identifier, result.getOccurredException());
        }

        return result;
    }

    @Override
    public PlaybackChangeResult pause() {
        if (audioPlayer.getPlayingTrack() == null) {
            return PlaybackChangeResult.NOTHING_TO_CHANGE;
        } else if (audioPlayer.isPaused()) {
            return PlaybackChangeResult.NOT_DONE;
        } else {
            audioPlayer.setPaused(true);
            return PlaybackChangeResult.DONE;
        }
    }

    @Override
    public PlaybackChangeResult resume() {
        if (audioPlayer.getPlayingTrack() == null) {
            return PlaybackChangeResult.NOTHING_TO_CHANGE;
        } else if (audioPlayer.isPaused()) {
            audioPlayer.setPaused(false);
            return PlaybackChangeResult.DONE;
        } else {
            return PlaybackChangeResult.NOT_DONE;
        }
    }

    @Override
    public Optional<AudioTrack> playNext() {
        boolean playingNextTrack = tryPlayNextTrack();
        if (playingNextTrack) {
            audioPlayer.setPaused(false); // force unpause
            return Optional.of(audioPlayer.getPlayingTrack());
        }
        return Optional.empty();
    }

    @Override
    public Collection<AudioTrack> clearQueue() {
        List<AudioTrack> queueCopy = List.copyOf(queuedTracks);
        queuedTracks.clear();
        return queueCopy;
    }

    @Override
    public Collection<AudioTrack> getQueue() {
        return queuedTracks;
    }

    @Override
    public void close() {
        cancelAutoDisconnectTask();
        audioManager.closeAudioConnection();
        audioManager.setSendingHandler(null);
        queuedTracks.clear();
        audioPlayer.destroy();
    }

    private void ensureConnectedTo(VoiceChannel voiceChannel) {
        // for not connected case connectedChannel will be null
        if (!voiceChannel.equals(audioManager.getConnectedChannel())) {
            // if it's not a specified channel
            audioManager.openAudioConnection(voiceChannel);
        }
    }

    private boolean tryPlayNextTrack() {
        return audioPlayer.startTrack(queuedTracks.poll(), false);
    }

    private void cancelAutoDisconnectTask() {
        if (abandonedCheckerTask != null) {
            abandonedCheckerTask.cancel(true);
        }
    }

    @RequiredArgsConstructor
    private class DefaultAudioLoadResultHandler implements AudioLoadResultHandler {

        private final Reference<PlayActionResult> resultReference;

        @Override
        public void trackLoaded(AudioTrack track) {
            if (audioPlayer.startTrack(track, true)) {
                // started playing immediately, no need to queue
                resultReference.setObject(PlayActionResult.ofTrack(track, true));
            } else {
                queuedTracks.add(track);
                resultReference.setObject(PlayActionResult.ofTrack(track, false));
            }
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            List<AudioTrack> tracks = new ArrayList<>(playlist.getTracks()); // to be sure that collection isn't immutable
            AudioTrack track = tracks.get(0);
            if (audioPlayer.startTrack(track, true)) {
                // first track started playing immediately
                tracks.remove(0);
                queuedTracks.addAll(tracks);
                resultReference.setObject(PlayActionResult.ofPlaylist(playlist, true));
            } else {
                queuedTracks.addAll(tracks);
                resultReference.setObject(PlayActionResult.ofPlaylist(playlist, false));
            }
        }

        @Override
        public void noMatches() {
            resultReference.setObject(PlayActionResult.ofNothing());
        }

        @Override
        public void loadFailed(FriendlyException exception) {
            resultReference.setObject(PlayActionResult.ofException(exception));
        }
    }

    private class PlayNextTrackAudioEventListener extends AudioEventAdapter {

        @Override
        public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if (endReason.mayStartNext) {
                boolean startedNext = tryPlayNextTrack();
                if (startedNext) {
                    informer.informPlayingNextTrack(player.getPlayingTrack());
                }
            }
        }
    }

    private class AutoDisconnectAudioEventListener extends AudioEventAdapter {

        @Override
        public void onTrackStart(AudioPlayer player, AudioTrack track) {
            cancelAutoDisconnectTask();
        }

        @Override
        public void onPlayerResume(AudioPlayer player) {
            cancelAutoDisconnectTask();
        }

        @Override
        public void onPlayerPause(AudioPlayer player) {
            launchAutoDisconnectTask();
        }

        @Override
        public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if (queuedTracks.isEmpty() && player.getPlayingTrack() == null && !player.isPaused()) {
                launchAutoDisconnectTask();
            }
        }

        @Override
        public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
            launchAutoDisconnectTask();
        }

        private void launchAutoDisconnectTask() {
            abandonedCheckerTask = abandonedMonitor.schedule(informer::informAutoDisconnecting, AUTO_DISCONNECT_DELAY_MS,
                                                             TimeUnit.MILLISECONDS);
        }
    }
}
