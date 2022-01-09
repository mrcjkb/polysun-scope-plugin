package com.github.mrcjkb.polysun.plugin.controller.scope.api;

import java.util.List;
import java.util.Optional;

public interface IScopeModel<InputType> {

    void updateScopeData(int simulationTime, float[] inputData);

    Optional<Integer> getOptionalFixedTimestepSizeS();

    void forEachSeries(ScopeSeriesConsumer<InputType> consumer);

    @FunctionalInterface
    interface ScopeSeriesConsumer<InputType> {
        void accept(InputType input, List<Double> timeStamp, List<Double> yData);
    }
}
