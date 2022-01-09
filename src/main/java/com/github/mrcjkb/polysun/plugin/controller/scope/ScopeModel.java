package com.github.mrcjkb.polysun.plugin.controller.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScopeModel;

public class ScopeModel<InputType> implements IScopeModel<InputType> {

    private final List<InputType> inputList;
    private final Predicate<InputType> inputFilterPredicate;
    private final List<Double> timeStamp = new ArrayList<>();
    private final Map<InputType, List<Double>> ySeriesMap = new HashMap<>();
    private final Map<InputType, Double> runningSumsMap = new HashMap<>();
    private final Optional<Integer> optionalFixedTimestepSize;
    /** simulationTime from the last scope update */
	private int lastSimulationTime;


    public ScopeModel(List<InputType> inputList,
                      Predicate<InputType> inputFilterPredicate) {
        this(inputList, inputFilterPredicate, null);
    }

    public ScopeModel(List<InputType> inputList,
                      Predicate<InputType> inputFilterPredicate,
                      Integer optionalFixedTimestepSizeS) {
        this.inputList = inputList;
        this.inputFilterPredicate = inputFilterPredicate;
        this.optionalFixedTimestepSize = Optional.ofNullable(optionalFixedTimestepSizeS);
        initialiseYSeriesMap();
    }

    @Override
    public void updateScopeData(int simulationTime, float[] inputData) {
        double timestepWeight = computeTimestepWeight(simulationTime);
        if (isWriteTimestep(simulationTime)) {
            writeTimestepAndResetRunningSums(simulationTime, inputData, timestepWeight);
        } else {
            incrementRunningSums(inputData, timestepWeight);
        }
        lastSimulationTime = simulationTime;
    }

    @Override
    public void forEachSeries(ScopeSeriesConsumer<InputType> consumer) {
        ySeriesMap.forEach((input, ySeries) -> {
            if (inputFilterPredicate.test(input)) {
                consumer.accept(input, timeStamp, ySeries);
            }
        });
    }

    private void writeTimestepAndResetRunningSums(int simulationTime, float[] inputData, double timestepWeight) {
        timeStamp.add((double) simulationTime);
        inputList.stream()
            .filter(inputFilterPredicate)
            .forEach(input -> {
                int index = inputList.indexOf(input);
                float sensorValue = inputData[index];
                double runningSum = runningSumsMap.getOrDefault(input, 0D);
                runningSum += sensorValue * timestepWeight;
                List<Double> inputYData = ySeriesMap.getOrDefault(input, new ArrayList<>());
                inputYData.add(runningSum);
                ySeriesMap.put(input, inputYData);
                runningSumsMap.put(input, 0D); // reset running sum
            });
    }

	private void incrementRunningSums(float[] inputData, double timestepWeight) {
        inputList.stream()
            .filter(inputFilterPredicate)
            .forEach(input -> {
                int index = inputList.indexOf(input);
                float inputValue = inputData[index];
                double runningSum = runningSumsMap.getOrDefault(input, 0D);
                runningSum += inputValue * timestepWeight;
                runningSumsMap.put(input, runningSum);
            });
	}

    @Override
    public Optional<Integer> getOptionalFixedTimestepSizeS() {
        return optionalFixedTimestepSize;
    }

    private void initialiseYSeriesMap() {
        inputList.stream()
            .filter(inputFilterPredicate)
            .forEach(input -> ySeriesMap.put(input, new ArrayList<>()));
    }

    /**
	 * @param the simulation time passed down from the {@link #control(int, boolean, float[], float[], float[], boolean, Map)} method.
	 * @return {@code true} if the scope should write data
	 */
	private boolean isWriteTimestep(int simulationTime) {
        return optionalFixedTimestepSize
            .map(fixedTimestepSize -> simulationTime % fixedTimestepSize == 0)
            .orElse(true);
	}

    /**
	 * @param simulationTime simulation time in s
	 * @return the weight for data to be saved depending on the time step size (a value between 0 and 1)
	 */
	private double computeTimestepWeight(int simulationTime) {
        return optionalFixedTimestepSize
            .map(fixedTimestepSize -> (double) (simulationTime - lastSimulationTime) / fixedTimestepSize)
            .orElse(1D);
	}

}
