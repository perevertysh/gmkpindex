package com.gmkpindex
import kotlin.time.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.net.URL
import java.net.HttpURLConnection
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    var data = ""

    @OptIn(ExperimentalTime::class)
    val timeSource = TimeSource.Monotonic

    @OptIn(ExperimentalTime::class)
    var timestamp = timeSource.markNow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        this.fillText()
        val view = this.findViewById<TextView>(R.id.dataField)
        view.setText(this.data)
    }


    private fun sendGet(): String{
        val url = URL("https://services.swpc.noaa.gov/text/daily-geomagnetic-indices.txt")
        var last2Dates: MutableList<String>
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            val dates: List<String> = url.readText().split("\n").dropLast(1)
            last2Dates = dates.subList(dates.size - 2, dates.size)
                .joinToString("    ")
                .split("    ").toMutableList()
        }
        last2Dates[0] = this.applicationContext.getString(R.string.dateStr) + ": \n"  + last2Dates[0]
        last2Dates[4] = this.applicationContext.getString(R.string.dateStr) + ": \n"  + last2Dates[4]
        last2Dates[1] = this.applicationContext.getString(R.string.midStr) + ": \n"  + last2Dates[1]
        last2Dates[5] = this.applicationContext.getString(R.string.midStr) + ": \n"  + last2Dates[5]
        last2Dates[2] = this.applicationContext.getString(R.string.highStr) + ": \n"  + last2Dates[2]
        last2Dates[6] = this.applicationContext.getString(R.string.highStr) + ": \n"  + last2Dates[6]
        last2Dates[3] = this.applicationContext.getString(R.string.estStr) + ": \n" + last2Dates[3] + "\n"
        last2Dates[7] = this.applicationContext.getString(R.string.estStr) + ": \n" + last2Dates[7]

        return last2Dates.joinToString("\n")
    }

    private fun fillText(){
        thread {
            try {
                this.data = sendGet()
            } catch (e: Exception) {
                Log.e("Error", e.toString())
                this.data = ""
            }
        }
        Thread.sleep(1000)
    }

    @OptIn(ExperimentalTime::class)
    fun rewriteData(view: View){
        val timeDiff = this.timeSource.markNow() - this.timestamp
        if (timeDiff.inWholeSeconds >= 1800 || this.data.isEmpty()) {
            this.timestamp = this.timeSource.markNow()
            Log.i("TIME", timeDiff.toString())
            this.fillText()
            val view = this.findViewById<TextView>(R.id.dataField)
            if (!this.data.isEmpty()) {
                view.text = this.data
            }else{
                view.text = this.applicationContext.getString(R.string.noRes)
            }
        }
    }
}
