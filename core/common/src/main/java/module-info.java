import org.jspecify.annotations.NullMarked;

@NullMarked
module com.github.monaboiste.fairshare.common {
    requires static org.jspecify;

    exports com.github.monaboiste.fairshare.common;
    exports com.github.monaboiste.fairshare.common.commands;
    exports com.github.monaboiste.fairshare.common.domain;
    exports com.github.monaboiste.fairshare.common.events;
}
