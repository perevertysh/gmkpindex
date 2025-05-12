package com.gmkpindex

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gmkpindex.R.string.greetingTxt
import com.gmkpindex.ui.theme.GeoMagneticKpIndexTheme
import com.watbuy.watbuy.com.gmkpindex.MainApplication
import java.net.URL
import java.net.HttpURLConnection
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    var data = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        this.fillText()
        val view = this.findViewById<TextView>(R.id.dataField)
        view.setText(this.data)
    }

    private fun sendGet(): String {
        val url = URL("https://services.swpc.noaa.gov/text/daily-geomagnetic-indices.txt")
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            val dates: List<String> = url.readText().split("\n").dropLast(1)
            return dates.subList(dates.size - 2, dates.size)
                .joinToString("    ")
                .split("    ")
                .joinToString("\n")
        }
    }

    private fun fillText(){
        thread {
            try {
                this.data = sendGet()
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                this.data = "No response..."
            }
        }
        Thread.sleep(1000)
    }

    fun rewriteData(view: View){
        this.fillText()
        val view = this.findViewById<TextView>(R.id.dataField)
        view.text = this.data
    }
}
