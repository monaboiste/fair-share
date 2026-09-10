package com.softwarearchetypes.rules.core.config;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RuleDefinition(@Nullable UUID id, String name) {}
