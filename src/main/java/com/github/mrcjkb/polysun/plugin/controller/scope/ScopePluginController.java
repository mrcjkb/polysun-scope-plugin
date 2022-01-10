package com.github.mrcjkb.polysun.plugin.controller.scope;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScopeModel;
import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScopeView;
import com.velasolaris.plugin.controller.spi.AbstractPluginController;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration;
import com.velasolaris.plugin.controller.spi.PluginControllerException;
import com.velasolaris.plugin.controller.spi.PolysunSettings;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Log;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Property;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Sensor;

import static java.lang.String.format;

public class ScopePluginController extends AbstractPluginController {

    private static final Logger logger = Logger.getLogger(ScopePluginController.class.getName());

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;

	private static final String VARIABLE_TIME_STEP_SIZES_PROPERTY_KEY = "Plot variable time steps";
    private static enum YesNoOption {
        Yes,
        No
    }
	private static final String SCOPE_TIMESTEP_SIZE_PROPERTY_KEY = "Time step size / s";
    private static final int MINIMUM_SCOPE_TIME_STEP_SIZE_S = 1;
    private static final int MAXIMUM_SCOPE_TIME_STEP_SIZE_S = 3600;
    private static final int DEFAULT_SCOPE_TIME_STEP_SIZE_S = 900;
    protected static final int MAX_NUM_GENERIC_SENSORS = 30;
    private static final String SCOPE_UPDATE_INTERVAL_PROPERTY_KEY = "Scope update interval";
    private static enum ScopeViewUpdateIntervalOption {
        Realtime,
        Hourly,
        Daily
    }

    private Optional<IScopeModel<Sensor>> scopeModel = Optional.empty();
    private Optional<IScopeView<Sensor>> scopeView = Optional.empty();

    @Override
    public String getName() {
        return "Scope";
    }

    @Override
    public String getDescription() {
        return "Plots the sensor inputs to a scope during simulation.";
    }

    @Override
	public String getVersion() {
		return "1.0.0";
	}

    @Override
    public PluginControllerConfiguration getConfiguration(Map<String, Object> parameters)
        throws PluginControllerException {

        return new PluginControllerConfiguration.Builder()
            .setNumGenericSensors(MAX_NUM_GENERIC_SENSORS)
            .setNumGenericControlSignals(0)
            .setLogs(IntStream.range(0, MAX_NUM_GENERIC_SENSORS)
                    .mapToObj(logIndex -> new Log("Sensor " + logIndex))
                    .collect(Collectors.toList()))
            .setProperties(buildProperties())
            .build();
    }

    @Override
	public void build(PolysunSettings polysunSettings, Map<String, Object> parameters) throws PluginControllerException {
		super.build(polysunSettings, parameters);
        logger.info("Building...");
        logger.info("Sensors:");
        getSensors().forEach(sensor -> logger.info(sensor.toString()));
        scopeModel = Optional.of(isPlotVariableTimesteps()
            ? new ScopeModel<>(getSensors(), Sensor::isUsed)
            : new ScopeModel<>(getSensors(), Sensor::isUsed, getProperty(SCOPE_TIMESTEP_SIZE_PROPERTY_KEY).getInt()));
        scopeView.ifPresent(IScopeView::dispose);
        scopeView = Optional.of(new ScopeView<>(scopeModel.get(), sensor -> sensor.getName() + " / " + sensor.getUnit()));
	}

    @Override
    public void initialiseSimulation(Map<String, Object> parameters) throws PluginControllerException {
        super.initialiseSimulation(parameters);
        logger.info("Simulation started.");
        logParameters(Level.FINE, parameters);
        scopeView.ifPresent(IScopeView::show);
    }

    @Override
    public int[] control(int simulationTime, boolean status, float[] sensors, float[] controlSignals, float[] logValues,
            boolean preRun, Map<String, Object> parameters) throws PluginControllerException {

        // Show inputs in simulation analysis
        for (int i = 0; i < sensors.length; i++) {
            logValues[i] = sensors[i];
        }
		if (!preRun && status) {
            scopeModel.ifPresent(model -> model.updateScopeData(simulationTime, sensors));
        }
        if (isUpdateView(simulationTime)) {
            scopeView.ifPresent(IScopeView::update);
        }
        return null;
    }

    private boolean isUpdateView(int simulationTime) {
        int selectedScopeUpdateIntervalOptionIndex = getProperty(SCOPE_UPDATE_INTERVAL_PROPERTY_KEY).getInt();
        var scopeViewUpdateIntervalOption = ScopeViewUpdateIntervalOption.values()[selectedScopeUpdateIntervalOptionIndex];
        int scopeViewUpdateIntervalS = switch (scopeViewUpdateIntervalOption) {
            case Daily -> SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY;
            case Hourly -> SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
            case Realtime -> 1;
        };
        return simulationTime % scopeViewUpdateIntervalS == 0;
    }

    @Override
    public List<String> getPropertiesToHide(PolysunSettings polysunSettings, Map<String, Object> parameters) {
        List<String> propertiesToHide = super.getPropertiesToHide(polysunSettings, parameters);
        if (isPlotVariableTimesteps()) {
            propertiesToHide.add(SCOPE_TIMESTEP_SIZE_PROPERTY_KEY);
        }
        return propertiesToHide;
    }

    @Override
	public int getFixedTimestep(Map<String, Object> parameters) {
		return scopeModel.flatMap(IScopeModel::getOptionalFixedTimestepSizeS)
            .orElse(super.getFixedTimestep(parameters));
	}

    private static List<Property> buildProperties() {
        var variableTimeStepSizesProperty = new Property(VARIABLE_TIME_STEP_SIZES_PROPERTY_KEY,
                // Options
                enumToStringArray(YesNoOption.class),
                // Default option
                YesNoOption.Yes.ordinal(),
                // Tooltip
                """
                Yes: Plot inputs from each variable simulation time step.
                No: Average inputs to a constant time step size before plotting.
                """);
        var variableTimstepSizeProperty = new Property(SCOPE_TIMESTEP_SIZE_PROPERTY_KEY,
                DEFAULT_SCOPE_TIME_STEP_SIZE_S,
                MINIMUM_SCOPE_TIME_STEP_SIZE_S,
                MAXIMUM_SCOPE_TIME_STEP_SIZE_S,
                """
                The scope's time step size in seconds.
                Forces Polysun to limit the maximum time step size to the defined value.
                Smaller time steps than the maximum may still occur in the simulation.
                Warning: Setting a too small value may cause memory to run out during the simulation.
                """);
        var scopeUpdateIntervalProperty = new Property(SCOPE_UPDATE_INTERVAL_PROPERTY_KEY,
                enumToStringArray(ScopeViewUpdateIntervalOption.class),
                ScopeViewUpdateIntervalOption.Hourly.ordinal(),
                """
                How often to update the scope (simulation time)?
                Smaller update intervals may slow down the simulation.
                """);
        return List.of(variableTimeStepSizesProperty,
                variableTimstepSizeProperty,
                scopeUpdateIntervalProperty);
    }

    private boolean isPlotVariableTimesteps() {
        return getProp(VARIABLE_TIME_STEP_SIZES_PROPERTY_KEY)
            .getInt() == YesNoOption.Yes.ordinal();
    }

    private static <E extends Enum<E>> String[] enumToStringArray(Class<E> enumClass) {
        var stringArray = EnumSet.allOf(enumClass)
            .stream()
            .map(Object::toString)
            .collect(Collectors.toList())
            .toArray(String[]::new);
        logger.fine(format("Converted enum %s to String array: %s", enumClass.getSimpleName(), Arrays.toString(stringArray)));
        return stringArray;
    }

    private static void logParameters(Level logLevel, Map<String, Object> parameters) {
        parameters.forEach((key, value) -> logger.log(logLevel, format("Parameter %s: Value %s of type %s", key, value.toString(), value.getClass().getName())));
    }

}
