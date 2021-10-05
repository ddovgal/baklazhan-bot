package ua.ddovgal.baklazhan.audio;

import java.nio.ByteBuffer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;

import lombok.RequiredArgsConstructor;

import net.dv8tion.jda.api.audio.AudioSendHandler;

@RequiredArgsConstructor
public class AudioPlayerBackedAudioSendHandler implements AudioSendHandler {

    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer;
    private final MutableAudioFrame frame;

    public AudioPlayerBackedAudioSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        buffer = ByteBuffer.allocate(1024);
        frame = new MutableAudioFrame();
        frame.setBuffer(buffer);
    }

    @Override
    public boolean canProvide() {
        return audioPlayer.provide(frame);
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return buffer.flip();
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
