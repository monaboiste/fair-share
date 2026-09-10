package com.softwarearchetypes.rules.core.config;

import java.util.List;
import java.util.UUID;

public interface RuleDefinitionRepository {

    List<RuleDefinition> findAllDefinitions();

    List<RuleParam> findParamsByRuleId(UUID id);

    UUID insert(RuleDefinition definition);

    void insertParam(RuleParam param);
}
