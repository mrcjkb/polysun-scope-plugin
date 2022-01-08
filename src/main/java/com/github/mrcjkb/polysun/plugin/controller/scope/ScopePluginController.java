package com.github.mrcjkb.polysun.plugin.controller.scope;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScope;
import com.velasolaris.plugin.controller.spi.AbstractPluginController;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration;
import com.velasolaris.plugin.controller.spi.PluginControllerException;
import com.velasolaris.plugin.controller.spi.PolysunSettings;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Property;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Sensor;

import static java.lang.String.format;

public class ScopePluginController extends AbstractPluginController {

    private static final Logger logger = Logger.getLogger(ScopePluginController.class.getName());

	private static final String VARIABLE_TIME_STEP_SIZES_PROPERTY_KEY = "Plot variable time steps";
    private static enum YesNoOption {
        Yes,
        No
    }
	private static final String SCOPE_TIMESTEP_SIZE_PROPERTY_KEY = "Time step size / s";
    private static final int MINIMUM_SCOPE_TIME_STEP_SIZE_S = 1;
    private static final int MAXIMUM_SCOPE_TIME_STEP_SIZE_S = 900;
    private static final int DEFAULT_SCOPE_TIME_STEP_SIZE_S = MAXIMUM_SCOPE_TIME_STEP_SIZE_S;
    protected static final int MAX_NUM_GENERIC_SENSORS = 30;
    private static final String SCOPE_UPDATE_INTERVAL_PROPERTY_KEY = "Scope update interval";
    private static enum ScopeUpdateIntervalOption {
        Realtime,
        Hourly,
        Daily
    }

    private IScope<Sensor> scope;

    private Optional<Integer> optionalScopeTimestepSizeS;
    /** simulationTime from the last call of control() */
	private int lastSimulationTime;

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
            .setProperties(buildProperties())
            .build();
    }

    @Override
	public void build(PolysunSettings polysunSettings, Map<String, Object> parameters) throws PluginControllerException {
		super.build(polysunSettings, parameters);
		optionalScopeTimestepSizeS = isPlotVariableTimesteps()
            ? Optional.empty()
            : Optional.of(getProperty(SCOPE_TIMESTEP_SIZE_PROPERTY_KEY).getInt());
        this.scope = new Scope<>(getSensors());
	}

    @Override
    public void initialiseSimulation(Map<String, Object> parameters) throws PluginControllerException {
        super.initialiseSimulation(parameters);
        logger.info("Simulation started.");
        logParameters(Level.FINE, parameters);
    }

    @Override
    public int[] control(int simulationTime, boolean status, float[] sensors, float[] controlSignals, float[] logValues,
            boolean preRun, Map<String, Object> parameters) throws PluginControllerException {

        double timestepWeight = computeTimestepWeight(simulationTime);
		if (!preRun && status && isWriteTimestep(simulationTime)) {
            scope.updateScopeData(simulationTime, sensors, timestepWeight, Sensor::isUsed);
        } else if (!preRun && status) {
            scope.incrementRunningSums(sensors, timestepWeight, Sensor::isUsed);
        }
        return null;
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
		return optionalScopeTimestepSizeS.orElse(super.getFixedTimestep(parameters));
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
                enumToStringArray(ScopeUpdateIntervalOption.class),
                ScopeUpdateIntervalOption.Hourly.ordinal(),
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

    /**
	 * @param the simulation time passed down from the {@link #control(int, boolean, float[], float[], float[], boolean, Map)} method.
	 * @return {@code true} if the controller should write data
	 */
	protected boolean isWriteTimestep(int simulationTime) {
		return isPlotVariableTimesteps() ? true : simulationTime % getFixedTimestep(null) == 0;
	}

    /**
	 * @param simulationTime simulation time in s
	 * @return the weight for data to be saved depending on the time step size
	 */
	protected double computeTimestepWeight(int simulationTime) {
		return isPlotVariableTimesteps()
            ? 1D
            : (double) (simulationTime - lastSimulationTime) / getFixedTimestep(null);
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
