package com.github.monaboiste.fairshare.common.events;

public interface EventHandler {

    boolean supports(PublishedEvent event);

    void handle(PublishedEvent event);
}
