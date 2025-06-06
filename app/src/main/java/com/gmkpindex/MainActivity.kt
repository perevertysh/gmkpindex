package com.gmkpindex

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.HttpURLConnection
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


class ImpactMeta(val lvl: String, val desc: Int, val value: Int, val color: Int)

enum class ImpactEnum {
    CALM,
    MINOR,
    MODERATE,
    STRONG,
    SEVERE,
    EXTREME
}

val impactMap: Map<ImpactEnum, ImpactMeta> = mapOf(
    ImpactEnum.CALM to ImpactMeta("", R.string.g0Str, 0, R.color.calmClr),
    ImpactEnum.MINOR to ImpactMeta("G1", R.string.g1Str, 5, R.color.minorClr),
    ImpactEnum.MODERATE to ImpactMeta("G2", R.string.g2Str, 6, R.color.moderateClr),
    ImpactEnum.STRONG to ImpactMeta("G3", R.string.g3Str, 7, R.color.strongClr),
    ImpactEnum.SEVERE to ImpactMeta("G4", R.string.g4Str, 8, R.color.severeClr),
    ImpactEnum.EXTREME to ImpactMeta("G5", R.string.g5Str, 9, R.color.extremeClr),
)

class MainActivity : ComponentActivity() {

    var rawData = ""
    var data = ""

    @OptIn(ExperimentalTime::class)
    val timeSource = TimeSource.Monotonic

    @OptIn(ExperimentalTime::class)
    var timestamp = timeSource.markNow()
    override fun onStart() {
        super.onStart()
        rewriteData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val textView = this.findViewById<TextView>(R.id.dataField)
        val buttonView = this.findViewById<Button>(R.id.button)
        buttonView.setOnClickListener {
            refreshData(textView)
        }
        val buttonInfoView = this.findViewById<Button>(R.id.buttonInfo)
        buttonInfoView.setOnClickListener {
            showIdxInfo()
        }
        val infoScrollView = this.findViewById<ScrollView>(R.id.curIdxInfoScroll)
        infoScrollView.visibility = View.INVISIBLE
    }

    private fun getImpactMeta(geomagneticIdx: Double): ImpactMeta? {
        return if (geomagneticIdx < impactMap[ImpactEnum.MINOR]!!.value) {
                impactMap[ImpactEnum.CALM]
            } else if (impactMap[ImpactEnum.MINOR]!!.value <= geomagneticIdx &&
                geomagneticIdx < impactMap[ImpactEnum.MODERATE]!!.value
            ) {
                impactMap[ImpactEnum.MINOR]
            } else if (impactMap[ImpactEnum.MODERATE]!!.value <= geomagneticIdx &&
                geomagneticIdx < impactMap[ImpactEnum.STRONG]!!.value
            ) {
                impactMap[ImpactEnum.MODERATE]
            } else if (impactMap[ImpactEnum.STRONG]!!.value <= geomagneticIdx &&
                geomagneticIdx < impactMap[ImpactEnum.SEVERE]!!.value
            ) {
                impactMap[ImpactEnum.STRONG]
            }else if (impactMap[ImpactEnum.SEVERE]!!.value <= geomagneticIdx &&
                geomagneticIdx < impactMap[ImpactEnum.EXTREME]!!.value
            ) {
                impactMap[ImpactEnum.SEVERE]
            }
            else if  (impactMap[ImpactEnum.EXTREME]!!.value <= geomagneticIdx){
                impactMap[ImpactEnum.EXTREME]
            } else {
                impactMap[ImpactEnum.CALM]
            }
    }

    private fun parseData(dataStr: String): Pair<MutableList<String>, Double> {
        var last2Dates: MutableList<List<String>>
        var last2DatesData: MutableList<String> = mutableListOf()
        val pattern = resources.getString(R.string.regexpPattern).toRegex()
        val dates: List<String> =dataStr.split("\n").dropLast(1).toList()

        last2Dates = dates.subList(dates.size - 2, dates.size).map{
                d -> pattern.find(d)?.groupValues?.drop(1)
        } as MutableList<List<String>>
        val (yesterdayData, todayData) = last2Dates

        // previous day info
        last2DatesData.add(resources.getString(R.string.dateStr) + ": \n" + yesterdayData[0])
        last2DatesData.add(resources.getString(R.string.midStr) + ": \n" + yesterdayData[1])
        last2DatesData.add(resources.getString(R.string.highStr) + ": \n" + yesterdayData[2])
        val prevDateIdxs: String = yesterdayData[3].toString()
        last2DatesData.add(resources.getString(R.string.estStr) + ": \n" + yesterdayData[3] + "\n")

        // current day info
        last2DatesData.add(resources.getString(R.string.dateStr) + ": \n" + todayData[0])
        last2DatesData.add(resources.getString(R.string.midStr) + ": \n" + todayData[1])
        last2DatesData.add(resources.getString(R.string.highStr) + ": \n" + todayData[2])

        var rippedLastDateIdx: Double = -1.0

        for (idx in todayData[3].toString()
            .split(" ").asReversed().dropLast(1)) {
            if (
                idx != "-1.00" &&
                idx.contains(".")
            ) {
                rippedLastDateIdx = idx.toDouble()
                break
            }
        }
        if (rippedLastDateIdx == -1.0) {
            for (idx in prevDateIdxs.split(" ")
                .asReversed()
                .dropLast(1)) {
                if (
                    idx != "-1.00" &&
                    idx.contains(".")
                ) {
                    rippedLastDateIdx = idx.toDouble()
                    break
                }
            }
        }
        last2DatesData.add(this.applicationContext.getString(R.string.estStr) + ": \n" + todayData[3])
        return Pair(last2DatesData, rippedLastDateIdx)
    }


    private fun rewriteData(
        view: TextView = this@MainActivity.findViewById<TextView>(R.id.dataField)
    ) {
        val url = URL(resources.getString(R.string.dataURL))
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    with(url.openConnection() as HttpURLConnection) {
                        requestMethod = "GET"
                        connectTimeout = 1000
                        readTimeout = 1000
                        if (responseCode in 200..299) {
                            rawData = url.readText()
                            withContext(Dispatchers.Main) {
                                updateViews(view)
                            }
                        }else{
                            Log.e("HTTP_ERROR", responseCode.toString())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Error:", e.stackTraceToString())
                }
            }

        }
    }

    private fun updateViews(view: TextView){
        val noResStr = resources.getString(R.string.noRes)
        var mainView = this.findViewById<View>(R.id.activityMain)
        var curIdxView = this.findViewById<TextView>(R.id.curIdx)
        var curIdxNameView = this.findViewById<TextView>(R.id.curIdxName)
        var curIdxInfoView = this.findViewById<TextView>(R.id.curIdxInfo)
        if (!this.rawData.isEmpty()) {
            val(last2DatesData, rippedLastDateIdx) =
                parseData(this.rawData)
            var impactScl: ImpactMeta? = getImpactMeta(rippedLastDateIdx.toDouble())
            this.data = last2DatesData.joinToString("\n")
            view.text = this.data
            curIdxView.text = rippedLastDateIdx.toString()
            curIdxView.setTextColor(resources.getColor(impactScl!!.color))
            view.setBackgroundColor(resources.getColor(impactScl.color))
            mainView.setBackgroundColor(resources.getColor(impactScl.color))
            curIdxNameView.text = impactScl.lvl
            curIdxInfoView.text = HtmlCompat.fromHtml(
                resources.getString(impactScl.desc),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else {
            view.text = noResStr
        }
    }

    @OptIn(ExperimentalTime::class)
    fun refreshData(view: TextView) {
        val timeDiff = this.timeSource.markNow() - this.timestamp
        if (timeDiff.inWholeSeconds >= resources.getInteger(R.integer.refreshDelay) || this.rawData.isEmpty()) {
            this.timestamp = this.timeSource.markNow()
            Log.i("TIME", timeDiff.toString())
            rewriteData(view)
        }
    }

    private fun showIdxInfo() {
        val infoView = this.findViewById<ScrollView>(R.id.curIdxInfoScroll)
        if (infoView.isInvisible) {
            infoView.visibility = View.VISIBLE
        } else {
            infoView.visibility = View.INVISIBLE
        }
    }
}
