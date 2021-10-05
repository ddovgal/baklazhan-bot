package ua.ddovgal.baklazhan.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public interface AudioEventsInformer {
    void informPlayingNextTrack(AudioTrack track);
    void informAutoDisconnecting();
}
