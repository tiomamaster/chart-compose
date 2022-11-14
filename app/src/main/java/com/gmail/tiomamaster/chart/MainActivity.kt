package com.gmail.tiomamaster.chart

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gmail.tiomamaster.chart.compose.ChartData
import com.gmail.tiomamaster.chart.compose.MultitouchChart
import com.google.gson.Gson
import com.google.gson.JsonArray
import java.nio.charset.Charset
import java.text.SimpleDateFormat

@Suppress("UNCHECKED_CAST")
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val samplesStr = resources.openRawResource(R.raw.chart_data).readBytes()
            .toString(Charset.defaultCharset())
        val sampleDataNum = 4
        val sampleData = with(Gson()) {
            fromJson(
                fromJson(samplesStr, JsonArray::class.java)[sampleDataNum].asJsonObject,
                SampleData::class.java
            )
        }

        val x = (sampleData.columns!![0] as Iterable<*>).drop(1) as List<Long>
        val y = sampleData.columns.drop(1).map { (it as Iterable<*>).drop(1) as List<Long> }
        val colors = listOfNotNull(
            sampleData.colors?.y0,
            sampleData.colors?.y1,
            sampleData.colors?.y2,
            sampleData.colors?.y3
        ).map { Color(android.graphics.Color.parseColor(it)) }
        val labels = listOfNotNull(
            sampleData.names?.y0,
            sampleData.names?.y1,
            sampleData.names?.y2,
            sampleData.names?.y3
        )
        setContent {
            ChartWithPreviewDemo(ChartData(x, y, colors, labels))
        }
    }

    @Suppress("FunctionName")
    @SuppressLint("SimpleDateFormat")
    @Composable
    private fun ChartWithPreviewDemo(data: ChartData<*, *>) {
        val labelsFormatter = SimpleDateFormat("MMM dd")
        val detailsFormatter = SimpleDateFormat("EE, dd MMM yyyy")
        MultitouchChart(
            Modifier.padding(top = 16.dp, bottom = 16.dp),
            data,
            labelsFormatter::format,
            detailsFormatter::format
        )
    }
}
