package com.github.mrcjkb.polysun.plugin.controller.scope;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.velasolaris.plugin.controller.spi.AbstractPluginController;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration;
import com.velasolaris.plugin.controller.spi.PluginControllerException;
import com.velasolaris.plugin.controller.spi.PolysunSettings;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Property;

import static java.lang.String.format;

public class ScopePluginController extends AbstractPluginController {

    private static final Logger logger = Logger.getLogger(ScopePluginController.class.getName());

	private static final String VARIABLE_TIME_STEP_SIZES_PROPERTY_KEY = "Plot variable time steps";
    private static enum YesNoOption {
        Yes,
        No;
    }
	private static final String VARIABLE_TIMESTEP_SIZE_PROPERTY_KEY = "Time step size / s";
    private static final int MINIMUM_VARIABLE_TIME_STEP_SIZE_S = 1;
    private static final int MAXIMUM_VARIABLE_TIME_STEP_SIZE_S = 900;
    private static final int DEFAULT_VARIABLE_TIME_STEP_SIZE_S = MAXIMUM_VARIABLE_TIME_STEP_SIZE_S;
    protected static final int MAX_NUM_GENERIC_SENSORS = 30;

    private int fixedTimestep;
    private double[][] polysunSensorData;

    @Override
    public String getName() {
        return "Scope";
    }

    @Override
    public String getDescription() {
        return "Plots the sensor inputs to a scope during simulation.";
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
    public void initialiseSimulation(Map<String, Object> parameters) throws PluginControllerException {
        super.initialiseSimulation(parameters);
        logger.info("Simulation started.");
        parameters.forEach((key, value) -> logger.fine(format("Parameter %s: Value %s of type %s", key, value.toString(), value.getClass().getName())));
    }

    @Override
    public int[] control(int simulationTime, boolean status, float[] sensors, float[] controlSignals, float[] logValues,
            boolean preRun, Map<String, Object> parameters) throws PluginControllerException {

        if (preRun || sensors.length == 0)
            return null;
        return null;
    }

    @Override
    public List<String> getPropertiesToHide(PolysunSettings polysunSettings, Map<String, Object> parameters) {
        List<String> propertiesToHide = super.getPropertiesToHide(polysunSettings, parameters);
        if (isPlotVariableTimesteps()) {
            propertiesToHide.add(VARIABLE_TIMESTEP_SIZE_PROPERTY_KEY);
        }
        return propertiesToHide;
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
        var variableTimstepSizeProperty = new Property(VARIABLE_TIMESTEP_SIZE_PROPERTY_KEY,
                DEFAULT_VARIABLE_TIME_STEP_SIZE_S,
                MINIMUM_VARIABLE_TIME_STEP_SIZE_S,
                MAXIMUM_VARIABLE_TIME_STEP_SIZE_S,
                """
                The variable time step size in seconds.
                Forces Polysun to limit the maximum time step size to the defined value.
                Smaller time steps than the maximum may still occur in the simulation.
                Warning: Setting a too small value may cause memory to run out during the simulation.
                """);
        return List.of(variableTimeStepSizesProperty, variableTimstepSizeProperty);
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
        logger.info(format("Converted enum %s to String array: %s", enumClass.getSimpleName(), Arrays.toString(stringArray)));
        return stringArray;
    }
}
