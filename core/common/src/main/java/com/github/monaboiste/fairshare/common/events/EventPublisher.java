package com.github.monaboiste.fairshare.common.events;

import java.util.List;

public interface EventPublisher {

    void publish(PublishedEvent event);

    void publish(List<? extends PublishedEvent> events);

    void register(EventHandler eventHandler);
}
