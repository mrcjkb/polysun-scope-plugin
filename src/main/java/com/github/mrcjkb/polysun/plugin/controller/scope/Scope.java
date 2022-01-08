package com.github.mrcjkb.polysun.plugin.controller.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScope;

public class Scope<InputType> implements IScope<InputType> {

    private final List<InputType> inputList;
    private final List<Double> timeStamp = new ArrayList<>();
    private final Map<InputType, List<Double>> yData = new HashMap<>();
    private final Map<InputType, Double> runningSums = new HashMap<>();
    private final Optional<Integer> optionalFixedTimestepSize;
    /** simulationTime from the last scope update */
	private int lastSimulationTime;


    public Scope(List<InputType> inputList) {
        this(inputList, null);
    }

    public Scope(List<InputType> inputList, Integer optionalFixedTimestepSizeS) {
        this.inputList = inputList;
        this.optionalFixedTimestepSize = Optional.ofNullable(optionalFixedTimestepSizeS);
    }

    @Override
    public void updateScopeData(int simulationTime, float[] inputData, Predicate<InputType> inputFilterPredicate) {
        double timestepWeight = computeTimestepWeight(simulationTime);
        if (isWriteTimestep(simulationTime)) {
            writeTimestepAndResetRunningSums(simulationTime, inputData, timestepWeight, inputFilterPredicate);
        } else {
            incrementRunningSums(inputData, timestepWeight, inputFilterPredicate);
        }
        lastSimulationTime = simulationTime;
    }

    private void writeTimestepAndResetRunningSums(int simulationTime, float[] inputData, double timestepWeight, Predicate<InputType> inputFilterPredicate) {
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

	private void incrementRunningSums(float[] inputData, double timestepWeight, Predicate<InputType> inputFilterPredicate) {
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

    @Override
    public Optional<Integer> getOptionalFixedTimestepSizeS() {
        return optionalFixedTimestepSize;
    }

    /**
	 * @param the simulation time passed down from the {@link #control(int, boolean, float[], float[], float[], boolean, Map)} method.
	 * @return {@code true} if the scope should write data
	 */
	private boolean isWriteTimestep(int simulationTime) {
        return optionalFixedTimestepSize
            .map(fixedTimestepSize -> fixedTimestepSize == 0)
            .orElse(true);
	}

    /**
	 * @param simulationTime simulation time in s
	 * @return the weight for data to be saved depending on the time step size (a value between 0 and 1)
	 */
	protected double computeTimestepWeight(int simulationTime) {
        return optionalFixedTimestepSize
            .map(fixedTimestepSize -> (double) (simulationTime - lastSimulationTime) / fixedTimestepSize)
            .orElse(1D);
	}

}
