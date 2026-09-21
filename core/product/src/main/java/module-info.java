import org.jspecify.annotations.NullMarked;

@NullMarked
module com.github.monaboiste.fairshare.product {
    requires static org.jspecify;
    requires transitive com.github.monaboiste.fairshare.common;
    requires transitive com.github.monaboiste.fairshare.quantity;

    exports com.github.monaboiste.fairshare.product;
}
