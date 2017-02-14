/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.chromium.latency.walt;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class HistogramChart extends BarChart {

    private static final float BIN_WIDTH = 5;
    private final ArrayList<ArrayList<Double>> rawData;
    private final ArrayList<IBarDataSet> dataSets;

    private int minBin = 0;
    private int maxBin = 100;
    private double min = 0;
    private double max = 100;

    public HistogramChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HistogramChart);
        final String descString;
        final int numDataSets;
        try {
            descString = a.getString(R.styleable.HistogramChart_description);
            numDataSets = a.getInteger(R.styleable.HistogramChart_numDataSets, 1);
        } finally {
            a.recycle();
        }

        rawData = new ArrayList<>(numDataSets);
        dataSets = new ArrayList<>(numDataSets);
        for (int i = 0; i < numDataSets; i++) {
            rawData.add(new ArrayList<Double>());
            final BarDataSet dataSet = new BarDataSet(new ArrayList<BarEntry>(), "");
            dataSet.setColor(ColorTemplate.MATERIAL_COLORS[i]);
            dataSets.add(dataSet);
        }

        BarData barData = new BarData(dataSets);
        barData.setBarWidth(0.9f/numDataSets);
        setData(barData);
        groupBars(0, 0.08f, 0.01f);
        final Description desc = new Description();
        desc.setText(descString);
        desc.setTextSize(12f);
        setDescription(desc);

        XAxis xAxis = getXAxis();
        xAxis.setGranularityEnabled(true);
        xAxis.setGranularity(1);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            DecimalFormat df = new DecimalFormat("#.##");

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return df.format(value * BIN_WIDTH + minBin);
            }
        });

        setFitBars(true);
        invalidate();
    }

    public void clearData() {
        for (int i = 0; i < rawData.size(); i++) {
            rawData.get(i).clear();
            dataSets.get(i).clear();
        }
        invalidate();
    }

    void addEntry(int dataSetIndex, double value) {
        if (isRawDataEmpty()) {
            min = value;
            max = value;
        } else {
            if (value < min) min = value;
            if (value > max) max = value;
        }

        rawData.get(dataSetIndex).add(value);
        minBin = (int) (Math.floor(min/BIN_WIDTH)*BIN_WIDTH);
        maxBin = (int) (Math.floor(max/BIN_WIDTH)*BIN_WIDTH);

        final int numBins = (int) ((maxBin - minBin) / BIN_WIDTH) + 1;
        int[][] bins = new int[rawData.size()][numBins];

        for (int setNum = 0; setNum < rawData.size(); setNum++) {
            for (Double d : rawData.get(setNum)) {
                ++bins[setNum][(int) (Math.floor((d - minBin)/ BIN_WIDTH))];
            }
        }

        for (IBarDataSet dataSet : dataSets) {
            dataSet.clear();
        }

        for (int setNum = 0; setNum < dataSets.size(); setNum++) {
            for (int i = 0; i < bins[setNum].length; i++) {
                dataSets.get(setNum).addEntry(new BarEntry(i, bins[setNum][i]));
            }
        }

        mXAxis.setAxisMinimum(0);
        mXAxis.setAxisMaximum(numBins);
        groupBars(0, 0.08f, 0.01f);
        invalidate();
    }

    private boolean isRawDataEmpty() {
        for (ArrayList<Double> data : rawData) {
            if (!data.isEmpty()) return false;
        }
        return true;
    }

    public void setLabel(int dataSetIndex, String label) {
        dataSets.get(dataSetIndex).setLabel(label);
        getLegendRenderer().computeLegend(getBarData());
        invalidate();
    }
}