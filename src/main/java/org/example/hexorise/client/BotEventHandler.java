package org.example.hexorise.client;
import tools.jackson.databind.JsonNode;
@FunctionalInterface
public interface BotEventHandler {
    void handle(JsonNode event, String botUserId, HighriseClient client);
}
