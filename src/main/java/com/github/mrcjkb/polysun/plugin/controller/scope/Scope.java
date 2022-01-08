package com.github.mrcjkb.polysun.plugin.controller.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScope;

public class Scope<InputType> implements IScope<InputType> {

    private final List<InputType> inputList;
    private final List<Double> timeStamp = new ArrayList<>();
    private final Map<InputType, List<Double>> yData = new HashMap<>();
    private final Map<InputType, Double> runningSums = new HashMap<>();

    public Scope(List<InputType> inputList) {
        this.inputList = inputList;
    }

    @Override
    public void updateScopeData(int simulationTime, float[] inputData, double timestepWeight, Predicate<InputType> inputFilterPredicate) {
        timeStamp.add((double) simulationTime);
        inputList.stream()
            .filter(inputFilterPredicate)
            .forEach(input -> {
                int index = inputList.indexOf(input);
                float sensorValue = inputData[index];
                double runningSum = runningSums.getOrDefault(input, 0D);
                runningSum += sensorValue * timestepWeight;
                List<Double> inputYData = yData.getOrDefault(input, new ArrayList<>());
                inputYData.add(runningSum);
                yData.put(input, inputYData);
                runningSums.put(input, 0D); // reset running sum
            });
    }

	public void incrementRunningSums(float[] inputData, double timestepWeight, Predicate<InputType> inputFilterPredicate) {
        inputList.stream()
            .filter(inputFilterPredicate)
            .forEach(input -> {
                int index = inputList.indexOf(input);
                float inputValue = inputData[index];
                double runningSum = runningSums.getOrDefault(input, 0D);
                runningSum += inputValue * timestepWeight;
                runningSums.put(input, runningSum);
            });
	}

}
