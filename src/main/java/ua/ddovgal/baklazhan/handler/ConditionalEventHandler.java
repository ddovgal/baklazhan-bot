package ua.ddovgal.baklazhan.handler;

import net.dv8tion.jda.api.events.GenericEvent;

public abstract class ConditionalEventHandler<T extends GenericEvent> implements EventHandler<T> {

    @Override
    public void handle(T event) {
        if (isSuitable(event)) {
            handleSuitable(event);
        }
    }

    public abstract boolean isSuitable(T event);
    public abstract void handleSuitable(T event);
}
