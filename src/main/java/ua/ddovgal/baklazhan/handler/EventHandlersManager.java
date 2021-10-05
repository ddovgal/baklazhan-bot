package ua.ddovgal.baklazhan.handler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.IEventManager;
import ua.ddovgal.baklazhan.util.ReflectionUtils;

public class EventHandlersManager implements IEventManager {

    private final Map<Class<? extends GenericEvent>, List<EventHandler<GenericEvent>>> handlersMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void register(@NotNull Object listener) {
        EventHandler<?> handler = tryCast(listener);
        Class<? extends GenericEvent> eventClass = getHandlingClass(handler);

        List<EventHandler<GenericEvent>> handlers = handlersMap.get(eventClass);
        if (handlers == null) {
            handlers = new CopyOnWriteArrayList<>();
            handlersMap.put(eventClass, handlers);
        }
        handlers.add((EventHandler<GenericEvent>) handler);
    }

    @Override
    public void unregister(@NotNull Object listener) {
        EventHandler<?> handler = tryCast(listener);
        Class<? extends GenericEvent> eventClass = getHandlingClass(handler);

        List<EventHandler<GenericEvent>> handlers = handlersMap.get(eventClass);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    @Override
    public void handle(@NotNull GenericEvent event) {
        Class<? extends GenericEvent> eventClass = event.getClass();
        List<EventHandler<GenericEvent>> handlers = handlersMap.get(eventClass);
        if (handlers != null) {
            handlers.forEach(handler -> handler.handle(event));
        }
    }

    @Override
    public @NotNull List<Object> getRegisteredListeners() {
        return handlersMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static EventHandler<?> tryCast(Object object) {
        if (object instanceof EventHandler) {
            return (EventHandler<?>) object;
        }
        throw new IllegalArgumentException("Object must implement EventHandler");
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends GenericEvent> getHandlingClass(EventHandler<?> handler) {
        return (Class<? extends GenericEvent>) ReflectionUtils.getActualTypesForClass(handler.getClass(), EventHandler.class).get(0);
    }
}
