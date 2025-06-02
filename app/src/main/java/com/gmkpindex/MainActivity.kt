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

    var data = ""

    @OptIn(ExperimentalTime::class)
    val timeSource = TimeSource.Monotonic

    @OptIn(ExperimentalTime::class)
    var timestamp = timeSource.markNow()
    override fun onStart() {
        super.onStart()
        val textView = this.findViewById<TextView>(R.id.dataField)
        rewriteData(textView)
        textView.text = this.data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val textView = this.findViewById<TextView>(R.id.dataField)

        val buttonView = this.findViewById<Button>(R.id.button)
        buttonView.setOnClickListener {
            rewriteData(textView)
        }
        val buttonInfoView = this.findViewById<Button>(R.id.buttonInfo)
        buttonInfoView.setOnClickListener {
            showIdxInfo()
        }
        val infoScrollView = this.findViewById<ScrollView>(R.id.curIdxInfoScroll)
        infoScrollView.visibility = View.INVISIBLE
    }

    private fun sendGet(view: TextView) {
        val url = URL(resources.getString(R.string.dataURL))
        val noResStr = resources.getString(R.string.noRes)
        var last2Dates: MutableList<List<String>>
        var last2DatesData: MutableList<String> = mutableListOf()
        var mainView = this.findViewById<View>(R.id.activityMain)
        var curIdxView = this.findViewById<TextView>(R.id.curIdx)
        var curIdxNameView = this.findViewById<TextView>(R.id.curIdxName)
        var curIdxInfoView = this.findViewById<TextView>(R.id.curIdxInfo)
        val appContext = this.applicationContext
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    with(url.openConnection() as HttpURLConnection) {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 10000
                        if (responseCode in 200..299) {
                            val pattern = resources.getString(R.string.regexpPattern).toRegex()

                            val dates: List<String> =
                                url.readText().split("\n").dropLast(1).toList()

                            last2Dates = dates.subList(dates.size - 2, dates.size).map{
                                    d -> pattern.find(d)?.groupValues?.drop(1)
                                } as MutableList<List<String>>
                            val (yesterdayData, todayData) = last2Dates

                            // previous day info
                            last2DatesData.add(appContext.getString(R.string.dateStr) + ": \n" + yesterdayData[0])
                            last2DatesData.add(appContext.getString(R.string.midStr) + ": \n" + yesterdayData[1])
                            last2DatesData.add(appContext.getString(R.string.highStr) + ": \n" + yesterdayData[2])
                            val prevDateIdxs: String = yesterdayData[3].toString()
                            last2DatesData.add(appContext.getString(R.string.estStr) + ": \n" + yesterdayData[3] + "\n")

                            // current day info
                            last2DatesData.add(appContext.getString(R.string.dateStr) + ": \n" + todayData[0])
                            last2DatesData.add(appContext.getString(R.string.midStr) + ": \n" + todayData[1])
                            last2DatesData.add(appContext.getString(R.string.highStr) + ": \n" + todayData[2])

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
                            last2DatesData.add(appContext.getString(R.string.estStr) + ": \n" + todayData[3])

                            var impactScl: ImpactMeta? = impactMap[ImpactEnum.MINOR]
                            impactScl =
                                if (rippedLastDateIdx < impactMap[ImpactEnum.MINOR]!!.value) {
                                    impactMap[ImpactEnum.CALM]
                                } else if (impactMap[ImpactEnum.MINOR]!!.value <= rippedLastDateIdx &&
                                    rippedLastDateIdx < impactMap[ImpactEnum.MODERATE]!!.value
                                ) {
                                impactMap[ImpactEnum.MINOR]
                                } else if (impactMap[ImpactEnum.MODERATE]!!.value <= rippedLastDateIdx &&
                                    rippedLastDateIdx < impactMap[ImpactEnum.STRONG]!!.value
                                ) {
                                    impactMap[ImpactEnum.MODERATE]
                                } else if (impactMap[ImpactEnum.STRONG]!!.value <= rippedLastDateIdx &&
                                    rippedLastDateIdx < impactMap[ImpactEnum.SEVERE]!!.value
                                ) {
                                    impactMap[ImpactEnum.STRONG]
                                }else if (impactMap[ImpactEnum.SEVERE]!!.value <= rippedLastDateIdx &&
                                    rippedLastDateIdx < impactMap[ImpactEnum.EXTREME]!!.value
                                ) {
                                    impactMap[ImpactEnum.SEVERE]
                                }
                                else if  (impactMap[ImpactEnum.EXTREME]!!.value <= rippedLastDateIdx){
                                    impactMap[ImpactEnum.EXTREME]
                                } else {
                                    impactMap[ImpactEnum.CALM]
                                }

                            withContext(Dispatchers.Main) {
                                val dataStr = last2DatesData.joinToString("\n")
                                if (!dataStr.isEmpty()) {
                                    view.text = dataStr
                                    curIdxView.text = rippedLastDateIdx.toString()
                                    curIdxView.setTextColor(appContext.getColor(impactScl!!.color))
                                    view.setBackgroundColor(appContext.getColor(impactScl.color))
                                    mainView.setBackgroundColor(appContext.getColor(impactScl.color))
                                    curIdxNameView.text = impactScl.lvl
                                    curIdxInfoView.text = HtmlCompat.fromHtml(
                                        appContext.getString(impactScl.desc),
                                        HtmlCompat.FROM_HTML_MODE_LEGACY
                                    )
                                } else {
                                    view.text = noResStr
                                }
                                data = view.text.toString()
                            }
                        } else {
                            Log.e("HTTP ERROR", url.readText().toString())
                            view.text = noResStr
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("Error:", e.stackTraceToString())
                        view.text = noResStr
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun rewriteData(view: TextView) {
        val timeDiff = this.timeSource.markNow() - this.timestamp
        if (timeDiff.inWholeSeconds >= resources.getInteger(R.integer.refreshDelay) || this.data.isEmpty()) {
            this.timestamp = this.timeSource.markNow()
            Log.i("TIME", timeDiff.toString())
            sendGet(view)
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
