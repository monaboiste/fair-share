import org.jspecify.annotations.NullMarked;

@NullMarked
module com.github.monaboiste.fairshare.quantity {
    requires static org.jspecify;
    requires transitive java.money;
    requires org.javamoney.moneta;

    exports com.github.monaboiste.fairshare.quantity;
    exports com.github.monaboiste.fairshare.quantity.money;
}
