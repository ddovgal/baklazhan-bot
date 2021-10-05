package ua.ddovgal.baklazhan.handler.impl;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import ua.ddovgal.baklazhan.handler.ConditionalEventHandler;

public class PingPongEventHandler extends ConditionalEventHandler<GuildMessageReceivedEvent> {

    private static final String PING_MESSAGE_TEXT = "!ping";
    private static final String PONG_MESSAGE_TEXT = "pong!";

    @Override
    public boolean isSuitable(GuildMessageReceivedEvent event) {
        return event.getMessage().getContentRaw().equals(PING_MESSAGE_TEXT);
    }

    @Override
    public void handleSuitable(GuildMessageReceivedEvent event) {
        event.getMessage().reply(PONG_MESSAGE_TEXT).queue();
    }
}
