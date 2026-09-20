package com.github.monaboiste.fairshare.rules.discounting.fixture.config

import com.github.monaboiste.fairshare.rules.core.selection.RuleConfigProvider
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.ClientContext
import com.github.monaboiste.fairshare.rules.discounting.offer.OfferItem

interface ConfigProvider extends RuleConfigProvider<ClientContext, OfferItem> {}
