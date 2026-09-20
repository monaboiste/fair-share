package com.github.monaboiste.fairshare.rules.discounting.config.codecs;

import com.github.monaboiste.fairshare.rules.core.config.reflection.ValueCodec;
import java.util.List;

public final class Codecs {

    public static final List<ValueCodec> QUANTITY = List.of(new MoneyCodec(), new PercentageCodec());

    private Codecs() {}
}
