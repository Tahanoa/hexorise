package org.example.hexorise.client;
public class HighriseRejectedException extends RuntimeException {
    public HighriseRejectedException() { super("Highrise rejected the operation. Check bot permissions, target account and emote availability."); }
}
