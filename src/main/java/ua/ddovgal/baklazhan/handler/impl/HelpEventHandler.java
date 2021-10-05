package ua.ddovgal.baklazhan.handler.impl;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ua.ddovgal.baklazhan.handler.ConditionalEventHandler;

public class HelpEventHandler extends ConditionalEventHandler<GuildMessageReceivedEvent> {

    private static final String HELP_REQUEST_MESSAGE_TEXT = "!help";
    private static final String INSTRUCTION_MESSAGE_TEXT =
        "Type `X` to **Y**:\n" +
        "- `!help` *to*... see this message\n" +
        "- `!play <identifier>` to play audio accessed by the `identifier`\n" +
        "- `!pause` *to* pause current music playback, if currently playing\n" +
        "- `!resume` *to* resume playback, if have something to play\n" +
        "- `!next` *to* abort current track playback and play next track in a queue if one present\n" +
        "- `!queue` *to* see all tracks that are currently queued\n" +
        "- `!clear` *to* clear all queued tracks\n" +
        "- `!bye` *to* disconnect from the voice channel loosing all queued tracks\n" +
        "            (please use it when you're going to leave)";

    @Override
    public boolean isSuitable(GuildMessageReceivedEvent event) {
        return event.getMessage().getContentRaw().equals(HELP_REQUEST_MESSAGE_TEXT);
    }

    @Override
    public void handleSuitable(GuildMessageReceivedEvent event) {
        event.getMessage().reply(INSTRUCTION_MESSAGE_TEXT).queue();
    }
}
