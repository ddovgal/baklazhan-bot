package ua.ddovgal.baklazhan;

import java.util.Collection;
import java.util.List;

import javax.security.auth.login.LoginException;

import lombok.RequiredArgsConstructor;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import ua.ddovgal.baklazhan.handler.EventHandler;
import ua.ddovgal.baklazhan.handler.EventHandlersManager;

@RequiredArgsConstructor
public class BaklazhanBot {

    private static final Activity LISTENING_TO_ORDERS_ACTIVITY = Activity.listening("your orders");
    private static final Collection<GatewayIntent> INTENTS = List.of(
        GatewayIntent.GUILD_MESSAGES, // we need to listen to messages
        GatewayIntent.GUILD_VOICE_STATES // we need to work with voice channels
    );

    private final String token;
    private final Collection<EventHandler<?>> eventHandlers;

    public JDA buildAndStart() throws LoginException, InterruptedException {
        return JDABuilder
            .createLight(token, INTENTS)
            .setMemberCachePolicy(MemberCachePolicy.VOICE)
            .enableCache(CacheFlag.VOICE_STATE)
            .setActivity(LISTENING_TO_ORDERS_ACTIVITY)
            .setEventManager(new EventHandlersManager()) // as we work with our own EventHandler-s
            .addEventListeners((Object[]) eventHandlers.toArray(new EventHandler[0]))
            .build();
    }
}
