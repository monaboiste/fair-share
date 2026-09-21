import org.jspecify.annotations.NullMarked;

@NullMarked
module com.github.monaboiste.fairshare.graphs {
    requires static org.jspecify;
    requires org.jgrapht.core;

    exports com.github.monaboiste.fairshare.graphs;
    exports com.github.monaboiste.fairshare.graphs.scheduling;
    exports com.github.monaboiste.fairshare.graphs.scheduling.concurrency;
}
