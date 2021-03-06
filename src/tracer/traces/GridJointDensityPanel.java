/*
 * JointDensityPanel.java
 *
 * Copyright (c) 2002-2017 Alexei Drummond, Andrew Rambaut and Marc Suchard
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package tracer.traces;

import dr.app.gui.chart.*;
import dr.app.gui.util.CorrelationData;
import dr.inference.trace.*;
import dr.stats.Variate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel that displays a grid of correlation plots for multiple traces
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Guy Baele
 * @version $Id: CorrelationPanel.java,v 1.1.1.2 2006/04/25 23:00:09 rambaut Exp $
 */
public class GridJointDensityPanel extends TraceChartPanel {

//    private JCheckBox sampleCheckBox = new JCheckBox("Sample only");
    private JCheckBox pointsCheckBox = new JCheckBox("Show points");
//    private JCheckBox translucencyCheckBox = new JCheckBox("Use translucency");

    private final JGridChart correlationChart;
    private final JChartPanel chartPanel;
    private final CorrelationData correlationData;

    private ChartSetupDialog chartSetupDialog = null;

    private JToolBar toolBar;

    /**
     * Creates new CorrelationPanel
     */
    public GridJointDensityPanel(final JFrame frame) {
        super(frame);

        correlationChart = new JGridChart(1.0);
        correlationData = new CorrelationData();
        chartPanel = new JChartPanel(correlationChart, "", "", "");

        toolBar = createToolBar(frame);
    }

    public JChartPanel getChartPanel() {
        return chartPanel;
    }

    @Override
    protected ChartSetupDialog getChartSetupDialog() {
        if (chartSetupDialog == null) {
            chartSetupDialog = new ChartSetupDialog(getFrame(), true, true, true, true,
                    Axis.AT_MAJOR_TICK, Axis.AT_MAJOR_TICK, Axis.AT_ZERO, Axis.AT_MAJOR_TICK);
        }
        return chartSetupDialog;
    }

    @Override
    protected Settings getSettings() {
        return null;
    }

    @Override
    protected JToolBar getToolBar() {
        return toolBar;
    }

    protected JChart getChart() {
        return correlationChart;
    }

    private JToolBar createToolBar(final JFrame frame) {
        JToolBar toolBar = super.createToolBar();

//        sampleCheckBox.setOpaque(false);
//        sampleCheckBox.setFont(UIManager.getFont("SmallSystemFont"));
//        // todo make 'samples only' unchecked as default for ordinal types
//        sampleCheckBox.setSelected(true);
//        toolBar.add(sampleCheckBox);

        pointsCheckBox.setOpaque(false);
        pointsCheckBox.setFont(UIManager.getFont("SmallSystemFont"));
        toolBar.add(pointsCheckBox);

//        translucencyCheckBox.setOpaque(false);
//        translucencyCheckBox.setFont(UIManager.getFont("SmallSystemFont"));
//        toolBar.add(translucencyCheckBox);

        toolBar.add(new JToolBar.Separator(new Dimension(8, 8)));

        ActionListener listener = new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                setupTraces();
            }
        };
//        sampleCheckBox.addActionListener(listener);
        pointsCheckBox.addActionListener(listener);
//        translucencyCheckBox.addActionListener(listener);

        return toolBar;
    }

    @Override
    protected void setupTraces() {

        int traceCount = 0;
        for (TraceList tl : getTraceLists()) {
            for (String traceName : getTraceNames()) {
                int traceIndex = tl.getTraceIndex(traceName);
                Trace trace = tl.getTrace(traceIndex);
                TraceCorrelation td = tl.getCorrelationStatistics(traceIndex);

                if (td == null) {
                    // TraceCorrelations not generated yet so must be still computing ESSs etc.
                    setMessage("Waiting for analysis of traces to complete");
                    return;
                }

                if (!trace.getTraceType().isContinuous()) {
                    setMessage("Multiple continuous traits required for grid correlation.");
                    return;
                }

                traceCount ++;
            }
        }

        int rowCount = traceCount;
        int columnCount = traceCount;
        int plotCount = rowCount * columnCount;

        correlationChart.setDimensions(rowCount, columnCount);
//        correlationChart.setDimensions(0, 0);

        getChartPanel().getChart().removeAllPlots();

        correlationData.clear();

        for (TraceList tl : getTraceLists()) {
            for (String traceName : getTraceNames()) {

                int traceIndex = tl.getTraceIndex(traceName);
                Trace trace = tl.getTrace(traceIndex);
                TraceCorrelation td = tl.getCorrelationStatistics(traceIndex);

                if (td == null) {
                    // TraceCorrelations not generated yet so must be still computing ESSs etc.

                    getChartPanel().setXAxisTitle("");
                    getChartPanel().setYAxisTitle("");
                    setMessage("Waiting for analysis of traces to complete");
                    return;
                }

                if (trace != null) {
                    String name = tl.getTraceName(traceIndex);
                    if (getTraceLists().length > 1) {
                        name = tl.getName() + " - " + name;
                    }

                    List values = tl.getValues(traceIndex);

                    //collect all traceNames and values while looping here
                    correlationData.add(name, values);
                }
            }

            boolean showPoints = pointsCheckBox.isSelected();
//            sampleCheckBox.isSelected();
//            translucencyCheckBox.isSelected();

            //add another routine here for the correlation plot, now that all the data has been collected
            //adding this here and not yet combining data for multiple .log files
            //TODO combine for multiple .log files once it's working for a single .log file

            int y = 0;
            for (String one : correlationData.getTraceNames()) {
                int x = 0;
                for (String two : correlationData.getTraceNames()) {
                    Plot plot = new CorrelationPlot(two, correlationData.getDataForKey(one), correlationData.getDataForKey(two), showPoints);
                    //plot.setLineStyle(new BasicStroke(2.0f), currentSettings.palette[0]);
                    plot.setLocation(x, y);
                    getChartPanel().getChart().addPlot(plot);
                    x ++;
                }
                y ++;
            }

//            y = 0;
//            for (String one : correlationData.getTraceNames()) {
//                int x = 0;
//                for (String two : correlationData.getTraceNames()) {
//                    Plot plot = new ScatterPlot(two, correlationData.getDataForKey(one), correlationData.getDataForKey(two));
//                    plot.setMarkStyle(Plot.POINT_MARK, 1.0,
//                            new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER),
//                            new Color(16, 16, 64, translucencyCheckBox.isSelected() ? 32 : 255),
//                            new Color(16, 16, 64, translucencyCheckBox.isSelected() ? 32 : 255));
//                    plot.setLocation(x, y);
//                    getChartPanel().getChart().addPlot(plot);
//                    x ++;
//                }
//                y ++;
//            }
        }

//        correlationChart.setDimensions(rowCount, columnCount);

    }

    public String toString() {
        if (getChart().getPlotCount() == 0) {
            return "no plot available";
        }

        StringBuffer buffer = new StringBuffer();

        Plot plot = getChart().getPlot(0);
        Variate xData = plot.getXData();
        Variate yData = plot.getYData();

        buffer.append(getChartPanel().getXAxisTitle());
        buffer.append("\t");
        buffer.append(getChartPanel().getYAxisTitle());
        buffer.append("\n");

        for (int i = 0; i < xData.getCount(); i++) {
            buffer.append(String.valueOf(xData.get(i)));
            buffer.append("\t");
            buffer.append(String.valueOf(yData.get(i)));
            buffer.append("\n");
        }

        return buffer.toString();
    }

    @Override
    public JComponent getExportableComponent() {
        return getChartPanel();
    }
}
