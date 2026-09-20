// FIXME(MSZ): move com.github.monaboiste.fairshare
package com.github.monaboiste.fairshare.common.events;

import java.time.Instant;
import java.util.UUID;

public interface PublishedEvent {

    UUID id();

    String type();

    Instant occurredAt();
}
