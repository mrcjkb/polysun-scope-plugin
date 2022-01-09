package com.github.mrcjkb.polysun.plugin.controller.scope;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Window;

import javax.swing.SwingUtilities;

import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScopeModel;
import com.github.mrcjkb.polysun.plugin.controller.scope.api.IScopeView;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.ChartTheme;
import org.knowm.xchart.style.Styler.LegendPosition;

public class ScopeView<InputType> implements IScopeView<InputType> {

    private static final Logger logger = Logger.getLogger(ScopeView.class.getName());

    private final IScopeModel<InputType> scopeModel;
    private final Function<InputType, String> seriesNameFactory;
    private final XYChart chart;
    private Optional<SwingWrapper<XYChart>> swingWrapperOptional = Optional.empty();


    public ScopeView(IScopeModel<InputType> scopeModel, Function<InputType, String> seriesNameFactory) {
        this.scopeModel = scopeModel;
        this.seriesNameFactory = seriesNameFactory;
        chart = new XYChartBuilder()
            .xAxisTitle("simulation time / s")
            .yAxisTitle("")
            .theme(ChartTheme.GGPlot2)
            .build();
        chart.getStyler().setZoomEnabled(true);
        chart.getStyler().setZoomResetByDoubleClick(false);
        chart.getStyler().setZoomResetByButton(true);
        chart.getStyler().setLegendPosition(LegendPosition.OutsideS);
        scopeModel.forEachSeries(this::addSeries);
    }

    @Override
    public void show() {
        initialiseSwingWrapper();
        SwingUtilities.invokeLater(() -> swingWrapperOptional.ifPresent(SwingWrapper::displayChart));
    }

    @Override
    public void update() {
        scopeModel.forEachSeries(this::updateSeries);
        SwingUtilities.invokeLater(() -> swingWrapperOptional.ifPresent(SwingWrapper::repaintChart));
    }

    @Override
    public void disopse() {
        try {
            swingWrapperOptional.ifPresent(swingWrapper -> {
                Optional.ofNullable(SwingUtilities.getWindowAncestor(swingWrapper.getXChartPanel()))
                    .ifPresent(Window::dispose);
            });
        } catch (Throwable t) {
            logger.log(Level.INFO, "Could not dispose scope view. Already disposed?");
        }
    }

    private void initialiseSwingWrapper() {
        swingWrapperOptional = Optional.of(new SwingWrapper<XYChart>(chart));
    }

    private void addSeries(InputType input, List<Double> timeStamp, List<Double> ySeries) {
        var seriesName = seriesNameFactory.apply(input);
        if (timeStamp == null || ySeries == null
                || timeStamp.isEmpty() || ySeries.isEmpty()) {
            logger.info("Add empty series: " + seriesName);
            chart.addSeries(seriesName, List.of(0), List.of(0));
        } else {
            logger.info("Add series: " + seriesName);
            chart.addSeries(seriesName, timeStamp, ySeries);
        }
    }

    private void updateSeries(InputType input, List<Double> timeStamp, List<Double> ySeries) {
        var seriesName = seriesNameFactory.apply(input);
        logger.info("Update series: " + seriesName);
        chart.updateXYSeries(seriesName, timeStamp, ySeries, null);
    }

}
