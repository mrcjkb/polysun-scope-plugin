package com.github.mrcjkb.polysun.plugin.controller.scope.api;

import java.util.function.Predicate;

public interface IScope<InputType> {

    void updateScopeData(int simulationTime, float[] inputData, double timestepWeight, Predicate<InputType> inputFilterPredicate);

	void incrementRunningSums(float[] inputData, double timestepWeight, Predicate<InputType> inputFilterPredicate);

}
