package com.softwarearchetypes.rules.discounting.config.codecs;

import com.softwarearchetypes.rules.core.config.reflection.ValueCodec;
import java.util.List;

public final class Codecs {

    public static final List<ValueCodec> QUANTITY = List.of(new MoneyCodec(), new PercentageCodec());

    private Codecs() {}
}
