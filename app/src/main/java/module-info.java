import org.jspecify.annotations.NullMarked;

@NullMarked
module com.github.monaboiste.fairshare.app {
    requires static org.jspecify;
    requires transitive com.github.monaboiste.fairshare.quantity;
    requires transitive com.github.monaboiste.fairshare.pricing;
    requires com.github.monaboiste.fairshare.graphs;
    requires transitive java.money;

    exports com.github.monaboiste.fairshare.netting;
    exports com.github.monaboiste.fairshare.valuation;
}
