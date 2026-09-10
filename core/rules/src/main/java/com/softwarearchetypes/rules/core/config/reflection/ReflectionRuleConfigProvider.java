package com.softwarearchetypes.rules.core.config.reflection;

import com.softwarearchetypes.rules.core.Modifier;
import com.softwarearchetypes.rules.core.config.ConfigKeys;
import com.softwarearchetypes.rules.core.config.RuleDefinition;
import com.softwarearchetypes.rules.core.config.RuleDefinitionRepository;
import com.softwarearchetypes.rules.core.config.RuleParam;
import com.softwarearchetypes.rules.core.selection.CandidateRule;
import com.softwarearchetypes.rules.core.selection.RuleConfigProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReflectionRuleConfigProvider<C, T> implements RuleConfigProvider<C, T> {

    private final RuleDefinitionRepository repository;
    private final List<ValueCodec> codecs;

    public ReflectionRuleConfigProvider(RuleDefinitionRepository repository, List<ValueCodec> codecs) {
        this.repository = repository;
        this.codecs = List.copyOf(codecs);
    }

    @Override
    public List<CandidateRule<C, T>> load() {
        List<CandidateRule<C, T>> rules = new ArrayList<>();

        for (RuleDefinition definition : repository.findAllDefinitions()) {
            Map<String, String> params = toParamMap(repository.findParamsByRuleId(
                    Objects.requireNonNull(definition.id(), "Stored rule ID is required")));
            ReflectionBeanReader reader = new ReflectionBeanReader(params, codecs);

            @SuppressWarnings("unchecked")
            Modifier<T> modifier = reader.readBean(ConfigKeys.MODIFIER_PREFIX, Modifier.class);
            @SuppressWarnings("unchecked")
            Predicate<C> appliesTo = reader.readBean(ConfigKeys.SELECTION_PREDICATE_PREFIX, Predicate.class);

            rules.add(new CandidateRule<>(modifier, appliesTo));
        }

        return rules;
    }

    private Map<String, String> toParamMap(List<RuleParam> params) {
        return params.stream().collect(Collectors.toMap(RuleParam::paramName, RuleParam::paramValue));
    }
}
