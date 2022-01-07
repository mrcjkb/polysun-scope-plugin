package com.github.mrcjkb.polysun.plugin.controller;

import java.util.List;
import java.util.Map;

import com.github.mrcjkb.polysun.plugin.controller.scope.ScopePluginController;
import com.velasolaris.plugin.controller.spi.AbstractControllerPlugin;
import com.velasolaris.plugin.controller.spi.IPluginController;

public class ScopeControllerPlugin extends AbstractControllerPlugin {

    @Override
    public List<Class<? extends IPluginController>> getControllers(Map<String, Object> parameters) {
        return List.of(ScopePluginController.class);
    }

    @Override
    public String getCreator() {
        return "Marc Jakobi";
    }

    @Override
    public String getDescription() {
        return "Plots the sensor inputs to a scope during simulation.";
    }
}
