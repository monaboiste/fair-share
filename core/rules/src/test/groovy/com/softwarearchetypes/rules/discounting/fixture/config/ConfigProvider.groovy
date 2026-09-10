package com.softwarearchetypes.rules.discounting.fixture.config

import com.softwarearchetypes.rules.core.selection.RuleConfigProvider
import com.softwarearchetypes.rules.discounting.fixture.client.ClientContext
import com.softwarearchetypes.rules.discounting.offer.OfferItem

interface ConfigProvider extends RuleConfigProvider<ClientContext, OfferItem> {}
