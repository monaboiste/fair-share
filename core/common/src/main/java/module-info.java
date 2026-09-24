import org.jspecify.annotations.NullMarked;

@NullMarked
module com.github.monaboiste.fairshare.common {
    requires static org.jspecify;
    requires org.slf4j;

    exports com.github.monaboiste.fairshare.common;
    exports com.github.monaboiste.fairshare.common.commands;
    exports com.github.monaboiste.fairshare.common.domain;
    exports com.github.monaboiste.fairshare.common.events;
    exports com.github.monaboiste.fairshare.common.events.inmemory;
    exports com.github.monaboiste.fairshare.common.eventsourcing;
    exports com.github.monaboiste.fairshare.common.queries;
}
