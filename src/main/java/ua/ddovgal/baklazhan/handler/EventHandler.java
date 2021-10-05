package ua.ddovgal.baklazhan.handler;

import net.dv8tion.jda.api.events.GenericEvent;

public interface EventHandler<T extends GenericEvent> {
    void handle(T event);
}
