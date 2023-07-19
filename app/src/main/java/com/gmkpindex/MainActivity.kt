package com.gmkpindex

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gmkpindex.ui.theme.GeoMagneticKpIndexTheme

import java.net.URL
import java.net.HttpURLConnection
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private var data = ""

    override fun onStart(){
        super.onStart()
        thread{
            try {
                this.data = sendGet()
            }catch (e: Exception){
                Log.e("Error", e.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoMagneticKpIndexTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(this.data)
                }
            }
        }
    }
    private fun sendGet(): String {
        val url = URL("https://services.swpc.noaa.gov/text/daily-geomagnetic-indices.txt")
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            return url.readText().split("\n").dropLast(1).last()
        }
    }

}

@Composable
fun Greeting(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
    )
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    GeoMagneticKpIndexTheme {
//        Greeting(body)
//    }
//}