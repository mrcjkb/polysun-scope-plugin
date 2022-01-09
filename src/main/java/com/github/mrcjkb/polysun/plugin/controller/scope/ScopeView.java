package com.github.mrcjkb.polysun.plugin.controller.scope;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.awt.Window;

import javax.swing.SwingUtilities;

import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScopeModel;
import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScopeView;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.ChartTheme;

public class ScopeView<InputType> implements IScopeView<InputType> {

    private final IScopeModel<InputType> scopeModel;
    private final Function<InputType, String> legendFactory;
    private final XYChart chart;
    private final SwingWrapper<XYChart> swingWrapper;


    public ScopeView(IScopeModel<InputType> scopeModel, Function<InputType, String> legendFactory) {
        this.scopeModel = scopeModel;
        this.legendFactory = legendFactory;
        chart = new XYChartBuilder()
            .xAxisTitle("simulation time / s")
            .yAxisTitle("")
            .theme(ChartTheme.GGPlot2)
            .build();
        scopeModel.forEachSeries(this::addSeries);
        swingWrapper = new SwingWrapper<XYChart>(chart);
    }

    @Override
    public void show() {
        swingWrapper.displayChart();
    }

    @Override
    public void update() {
        scopeModel.forEachSeries(this::updateSeries);
        swingWrapper.repaintChart();
    }

    @Override
    public void disopse() {
        Optional.ofNullable(SwingUtilities.getWindowAncestor(swingWrapper.getXChartPanel()))
            .ifPresent(Window::dispose);
    }

    private void addSeries(InputType input, List<Double> timeStamp, List<Double> ySeries) {
        var seriesName = legendFactory.apply(input);
        chart.addSeries(seriesName, timeStamp, ySeries);
    }

    private void updateSeries(InputType input, List<Double> timeStamp, List<Double> ySeries) {
        var seriesName = legendFactory.apply(input);
        chart.updateXYSeries(seriesName, timeStamp, ySeries, null);
    }

}
