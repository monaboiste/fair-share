import org.jspecify.annotations.NullMarked;

@NullMarked
module com.github.monaboiste.fairshare.pricing {
    requires static org.jspecify;
    requires transitive com.github.monaboiste.fairshare.quantity;

    exports com.github.monaboiste.fairshare.pricing.calculation;
    exports com.github.monaboiste.fairshare.pricing.component;
}
