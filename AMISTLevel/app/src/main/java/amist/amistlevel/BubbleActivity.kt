package amist.amistlevel

import amist.amistlevel.ui.theme.AMISTLevelTheme
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import java.lang.Math.clamp
import kotlin.math.sqrt
import kotlin.text.format

class BubbleActivity : ComponentActivity(), SensorEventListener
{
    @SuppressLint( "SourceLockedOrientationActivity" )
    override fun onCreate( savedInstanceState:Bundle? )
    {
        super.onCreate( savedInstanceState )

        if( !IsTablet( resources.configuration ) )
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        sensorManager = getSystemService( SENSOR_SERVICE ) as SensorManager

        setContent()
        {
            BubbleScreen()
        }

        val pref:SharedPreferences = getPreferences( MODE_PRIVATE )
        usePercent.value = pref.getBoolean( PREF_USE_PERCENT, usePercent.value )

        val symbol:String = GetSymbol( usePercent.value, this )
        hAngleText.value = "0$symbol"
        vAngleText.value = "0$symbol"

        hAngle.floatValue = 0f
        vAngle.floatValue = 0f
    }


    override fun onSensorChanged( sensorEvent:SensorEvent? )
    {
        if( sensorEvent != null )
        {
            when( sensorEvent.sensor.type )
            {
                Sensor.TYPE_ACCELEROMETER -> accValues = sensorEvent.values.clone()
                Sensor.TYPE_MAGNETIC_FIELD -> magValues = sensorEvent.values.clone()
            }

            var rotation:Int = windowManager.defaultDisplay.rotation
            if( !IsTablet( resources.configuration ) )
                rotation = Surface.ROTATION_0

            SetAngleValues( accValues,
                            magValues,
                            rotation,
                            this )
        }
    }


    override fun onResume()
    {
        super.onResume()

        sensorManager.getDefaultSensor( Sensor.TYPE_ACCELEROMETER )?.also{
            accelerometer -> sensorManager.registerListener( this,
                                                                accelerometer,
                                                                SensorManager.SENSOR_DELAY_NORMAL,
                                                                SensorManager.SENSOR_DELAY_UI )
        }

        sensorManager.getDefaultSensor( Sensor.TYPE_MAGNETIC_FIELD )?.also{
            magneticField -> sensorManager.registerListener( this,
                                                                magneticField,
                                                                SensorManager.SENSOR_DELAY_NORMAL,
                                                                SensorManager.SENSOR_DELAY_UI )
        }
    }

    override fun onPause()
    {
        super.onPause()

        sensorManager.unregisterListener( this )

        getPreferences( MODE_PRIVATE ).edit{
            putBoolean( PREF_USE_PERCENT, usePercent.value )
        }
    }


    lateinit var sensorManager:SensorManager

    var accValues:FloatArray = FloatArray( 3 )
    var magValues:FloatArray = FloatArray( 3 )


    override fun onAccuracyChanged( p0:Sensor?, p1:Int )    {}
}


val levelHeight:Dp = 60.dp
val levelStrokeWidth:Dp = 1.dp


@Composable
fun LevelHorizontal()
{
    Canvas( modifier = Modifier.fillMaxWidth().height( height = levelHeight ).padding( padding ) )
    {
        drawRoundRect( color = Color.White, cornerRadius = CornerRadius( size.height * 0.5f ), style = Stroke( width = levelStrokeWidth.toPx() ) )
        drawCircle( color = Color.White, radius = size.minDimension * 0.5f, style = Stroke( width = levelStrokeWidth.toPx() ) )

        val jcHAngle by hAngle
        translate( left = ( ( size.maxDimension - size.minDimension ) * 0.5f ) * clamp( jcHAngle / 30f, -1f, 1f ) )
        {
            drawCircle( color = Color.White, radius = size.minDimension * 0.5f, style = Fill )
        }
    }

    Box( modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter )
    {
        val jcHAngleText by hAngleText
        Text( text = jcHAngleText )
    }
}

@Composable
fun LevelVertical()
{
    Row()
    {
        Canvas( modifier = Modifier.width( width = levelHeight ).fillMaxHeight().padding( padding ) )
        {
            drawRoundRect( color = Color.White, cornerRadius = CornerRadius( size.width * 0.5f ), style = Stroke( width = levelStrokeWidth.toPx() ) )
            drawCircle( color = Color.White, radius = size.minDimension * 0.5f, style = Stroke( width = levelStrokeWidth.toPx() ) )

            val jcVAngle by vAngle
            translate( top = ( ( size.maxDimension - size.minDimension ) * 0.5f ) * clamp( jcVAngle / 30f, -1f, 1f ) )
            {
                drawCircle( color = Color.White, radius = size.minDimension * 0.5f, style = Fill )
            }
        }

        Box( modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center )
        {
            val jcVAngleText by vAngleText
            Text( text = jcVAngleText,
                    modifier = Modifier.layout
                    {
                        measurable,
                        constraints -> val placeable = measurable.measure( constraints )
                                        layout( placeable.height, placeable.width )
                                        {
                                            placeable.place( x = -( placeable.width - placeable.height ) / 2,
                                                                y = -( placeable.height - placeable.width ) / 2 )
                                        }
                    }.rotate( 90f ) )
        }
    }
}

@Composable
fun LevelFlat()
{
    Canvas( modifier = Modifier.fillMaxSize().padding( padding ) )
    {
        drawCircle( Color.White, radius = size.minDimension * 0.5f, style = Stroke( width = levelStrokeWidth.toPx() ) )

        val innerCircleSize: Float = ( levelHeight - ( padding * 2 ) ).toPx() * 0.5f
        drawCircle( Color.White, radius = innerCircleSize, style = Stroke( width = levelStrokeWidth.toPx() ) )

        val jcHAngle by hAngle
        val jcVAngle by vAngle
        var length:Float = sqrt( ( jcHAngle * jcHAngle ) + ( jcVAngle * jcVAngle ) )
        if( length > 1f )
            length = 1f / length
        else
            length = 1f

        val maxDist:Float = ( ( size.minDimension - innerCircleSize - ( padding.toPx() * 2f ) ) * 0.5f ) * length
        translate( left = maxDist * jcHAngle,
                    top = maxDist * jcVAngle )
        {
            drawCircle( Color.White, radius = innerCircleSize, style = Fill )
        }
    }
}

@Composable
fun BubbleLevel()
{
    Column()
    {
        LevelHorizontal()

        Row()
        {
            LevelVertical()
            LevelFlat()
        }
    }
}

@Composable
fun BubbleScreen()
{
    AMISTLevelTheme()
    {
        Surface( modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = Color.White )
        {
            Column()
            {
                AppBar()
                BubbleLevel()
            }
        }
    }
}

@Preview( showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode" )
@Composable
fun BubbleLevelPreview()
{
    BubbleScreen()

    hAngle.floatValue = 17.5f
    hAngleText.value = "%.1f%s".format( hAngle.floatValue, "%" )
    vAngle.floatValue = 42f
    vAngleText.value = "%.1f%s".format( vAngle.floatValue, "%" )
}