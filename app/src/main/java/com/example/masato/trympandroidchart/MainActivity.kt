package com.example.masato.trympandroidchart

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener{
    var pressure=0f
    var XAxisWidth=10f
    var autoScroll=true

    val METER=false
    val HECTOPASCAL=true
    var unitSelect=METER

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type==Sensor.TYPE_PRESSURE) {
            pressure = event.values.get(0)
        }
    }

    fun calcAltimeter(hectopascal :Float):Float{
        val p0=1013.25
        val temperture=20.0
        val h=(Math.pow(p0/hectopascal, 1/5.257)-1)*(temperture+273.15)/0.0065
        return h.toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val unitSwitch=findViewById<Switch>(R.id.unitSwitch)
        unitSwitch.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                unitSelect=isChecked
            }

        })

        val XAxisWidthSeekBar=findViewById<SeekBar>(R.id.seekBar)
        XAxisWidthSeekBar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                XAxisWidth=progress.toFloat()
            }

        })

        val sensorManager=getSystemService(SENSOR_SERVICE) as SensorManager;
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),SensorManager.SENSOR_DELAY_NORMAL)

        val autoScrollSwitch=findViewById<Switch>(R.id.autoScrollSwitch)
        autoScrollSwitch.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                autoScroll=isChecked
            }

        })

        val lineChart=findViewById<LineChart>(R.id.chart)
        lineChart.isDragEnabled=true
        lineChart.setTouchEnabled(true)
        lineChart.legend.isEnabled=false
        lineChart.description.isEnabled=false

        val entries= arrayListOf<Entry>()
        val correspondedDate=arrayListOf<Calendar>()

        val lineDataSet=LineDataSet(entries,"Label")

        lineDataSet.color= Color.BLACK
        lineDataSet.lineWidth=3f
        lineDataSet.setDrawCircles(false)

        lineDataSet.axisDependency=YAxis.AxisDependency.LEFT

        lineDataSet.valueFormatter=object:IValueFormatter{
            override fun getFormattedValue(
                value: Float,
                entry: Entry?,
                dataSetIndex: Int,
                viewPortHandler: ViewPortHandler?
            ): String {
                //return "123"
                return "%1$.2f".format(value)
            }

        }

        val lineData=LineData(lineDataSet)
        lineChart.data=lineData

        lineChart.axisLeft.isEnabled=true
        lineChart.axisRight.isEnabled=false
        lineChart.axisLeft.valueFormatter=object :IAxisValueFormatter{
            override fun getFormattedValue(value: Float, axis: AxisBase?): String {
                if(unitSelect==METER){
                    return "%1$.3f".format(calcAltimeter(value))
                }else if(unitSelect==HECTOPASCAL){
                    return "%1$.2f".format(value)
                }
                return "ERR"
            }

        }



        lineChart.xAxis.valueFormatter=object:IAxisValueFormatter{
            override fun getFormattedValue(value: Float, axis: AxisBase?): String {
                try {
                    val sdf=SimpleDateFormat("HH:mm:ss")
                    return sdf.format(correspondedDate[value.toInt()].time)
                    //return correspondedDate.get(value.toInt()).get(Calendar.SECOND).toString()
                }catch(e:ArrayIndexOutOfBoundsException){
                    return "ERR"
                }

            }

        }

        lineChart.setDragEnabled(true)


        lineChart.invalidate()

        val handler=Handler()
        val runnable=object : Runnable{
            var i=0
            override fun run() {
                val time=i
                correspondedDate.add(i, Calendar.getInstance())

                //val value=Math.random().toFloat()*1000
                val value=pressure

                val valueTextView=findViewById<TextView>(R.id.value)
                valueTextView.text="%1$.2f hPa".format(value)

                val variationTextView=findViewById<TextView>(R.id.variation)
                var variation=0f
                try {
                    variation=value-entries.last().y
                }catch(e:NoSuchElementException){}

                if(variation>=0){
                    variationTextView.setTextColor(Color.RED)
                }else{
                    variationTextView.setTextColor(Color.BLUE)
                }

                variationTextView.text="(%1$+.2f hPa)".format(variation)

                entries.add(Entry(time.toFloat(),value))

                lineDataSet.notifyDataSetChanged()
                lineData.notifyDataChanged()
                lineChart.notifyDataSetChanged()

                if(autoScroll) pursueView(lineChart,entries)

                lineChart.invalidate()

                handler.postDelayed(this,1000)
                i++
            }
        }
        handler.post(runnable)
    }

    fun pursueView(lineChart: LineChart,entries:ArrayList<Entry>){
        lineChart.xAxis.axisMinimum=lineChart.data.xMax-XAxisWidth.toInt()

        entries.takeLast(XAxisWidth.toInt()).minBy { e -> e.y }
        val recentYMax=entries.takeLast(XAxisWidth.toInt()).minBy { e -> e.y }?.y ?: 0f//エルビス演算子 ?: の左がnullならば右を返す
        val recentYMin=entries.takeLast(XAxisWidth.toInt()).maxBy { e -> e.y }?.y ?: 0f
        val margin=(recentYMax-recentYMin)*0.1f
        lineChart.axisLeft.axisMinimum=recentYMax+margin
        lineChart.axisLeft.axisMaximum=recentYMin-margin


    }
}
