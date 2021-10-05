package ua.ddovgal.baklazhan.handler;

import java.util.Optional;

import net.dv8tion.jda.api.events.GenericEvent;

public abstract class SharingConditionalEventHandler<T extends GenericEvent, S> implements EventHandler<T> {

    @Override
    public void handle(T event) {
        checkSuitability(event).ifPresent(shared -> handleSuitable(event, shared));
    }

    public abstract Optional<S> checkSuitability(T event);
    public abstract void handleSuitable(T event, S shared);
}
