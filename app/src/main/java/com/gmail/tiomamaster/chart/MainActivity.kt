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
import com.gmail.tiomamaster.chart.compose.ChartWithPreview
import com.gmail.tiomamaster.chart.view.Chart
import com.google.gson.Gson
import java.nio.charset.Charset
import java.text.SimpleDateFormat

@Suppress("UNCHECKED_CAST")
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val columnStr = resources.openRawResource(
            resources.getIdentifier("column", "raw", packageName)
        ).readBytes().toString(Charset.defaultCharset())
        val gson = Gson()
        val chart = gson.fromJson(columnStr, Chart::class.java)
        val x = (chart?.columns?.get(0) as? Iterable<*>)?.drop(1) as? List<Long>
        val y = (chart?.columns?.get(1) as? Iterable<*>)?.drop(1) as? List<Long>
        val y2 = (chart?.columns?.get(2) as? Iterable<*>)?.drop(1) as? List<Long>
        val y3 = (chart?.columns?.get(3) as? Iterable<*>)?.drop(1) as? List<Long>
        val y4 = (chart?.columns?.get(4) as? Iterable<*>)?.drop(1) as? List<Long>
//        val v = ChartView(this)
//        setContentView(v)
//        v.chart = chart
        setContent {
            ChartWithPreviewDemo(
                ChartData(
                    x!!,
                    listOf(y!!, y2!!, y3!!, y4!!),
                    listOf(Color.Blue, Color.Red, Color.Green, Color.Magenta),
                    listOf("1", "2", "3", "4")
                )
            )
        }
    }

    @Suppress("FunctionName")
    @SuppressLint("SimpleDateFormat")
    @Composable
    private fun ChartWithPreviewDemo(data: ChartData<Number, Number>) {
        val labelsFormatter = SimpleDateFormat("MMM dd")
        val detailsFormatter = SimpleDateFormat("EE, dd MMM yyyy")
        ChartWithPreview(
            Modifier.padding(top = 16.dp, bottom = 16.dp),
            data,
            labelsFormatter::format,
            detailsFormatter::format
        )
    }
}
