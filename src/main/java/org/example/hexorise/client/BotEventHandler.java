package org.example.hexorise.client;
import tools.jackson.databind.JsonNode;
@FunctionalInterface
public interface BotEventHandler {
    default void onSessionStarted(String botUserId, HighriseClient client) {}
    default void onSessionEnded(HighriseClient client) {}
    void handle(JsonNode event, String botUserId, HighriseClient client);
}
