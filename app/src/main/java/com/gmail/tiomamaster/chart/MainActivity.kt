package com.gmail.tiomamaster.chart

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val columnStr = resources.openRawResource(
            resources.getIdentifier("column", "raw", packageName)
        ).readBytes().toString(Charset.defaultCharset())
        val gson = Gson()
        val chart = gson.fromJson(columnStr, Chart::class.java)
        val v = ChartView(this)
        setContentView(v)
        v.chart = chart
    }
}
