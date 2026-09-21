import org.jspecify.annotations.NullMarked;

@NullMarked
module com.github.monaboiste.fairshare.rules {
    requires static org.jspecify;
    requires transitive com.github.monaboiste.fairshare.quantity;

    exports com.github.monaboiste.fairshare.rules.core;
    exports com.github.monaboiste.fairshare.rules.core.config;
    exports com.github.monaboiste.fairshare.rules.core.config.reflection;
    exports com.github.monaboiste.fairshare.rules.core.predicates;
    exports com.github.monaboiste.fairshare.rules.core.selection;
    exports com.github.monaboiste.fairshare.rules.discounting;
    exports com.github.monaboiste.fairshare.rules.discounting.config.codecs;
    exports com.github.monaboiste.fairshare.rules.discounting.offer;
    exports com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.applier;
    exports com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.guardians;
    exports com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.predicates;
    exports com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.simple;
    exports com.github.monaboiste.fairshare.rules.discounting.stock;
    exports com.github.monaboiste.fairshare.scoring;
    exports com.github.monaboiste.fairshare.scoring.algebra;
    exports com.github.monaboiste.fairshare.scoring.algebra.bool;
    exports com.github.monaboiste.fairshare.scoring.algebra.explained;
    exports com.github.monaboiste.fairshare.scoring.algebra.fuzzy;
    exports com.github.monaboiste.fairshare.scoring.algebra.score;
    exports com.github.monaboiste.fairshare.scoring.algebra.score.simplified;
    exports com.github.monaboiste.fairshare.scoring.ast;
    exports com.github.monaboiste.fairshare.scoring.context;
    exports com.github.monaboiste.fairshare.scoring.customer;
}
