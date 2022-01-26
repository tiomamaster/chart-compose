package com.gmail.tiomamaster.chart

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import java.nio.charset.Charset

class MainActivity : androidx.activity.ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val columnStr = resources.openRawResource(
            resources.getIdentifier("column", "raw", packageName)
        ).readBytes().toString(Charset.defaultCharset())
        val gson = Gson()
        val chart = gson.fromJson(columnStr, Chart::class.java)
        val x = (chart?.columns?.get(0) as? Iterable<*>)?.drop(1) as? List<Long>
        val y = (chart?.columns?.get(1) as? Iterable<*>)?.drop(1) as? List<Long>
//        val v = ChartView(this)
//        setContentView(v)
//        v.chart = chart
        setContent {
            ChartWithPreview(ChartData(x!!, y!!), Modifier.padding(16.dp))
        }
    }
}
