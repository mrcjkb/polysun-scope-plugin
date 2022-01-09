package com.github.mrcjkb.polysun.plugin.controller.scope.api;

import java.util.Optional;
import java.util.function.Predicate;

public interface IScopeModel<InputType> {

    void updateScopeData(int simulationTime, float[] inputData, Predicate<InputType> inputFilterPredicate);

    Optional<Integer> getOptionalFixedTimestepSizeS();

}
