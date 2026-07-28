package amist.amistlevel

import amist.amistlevel.ui.theme.AMISTLevelTheme
import android.Manifest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import android.content.SharedPreferences
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.cos
import kotlin.math.sin

class LaserActivity : ComponentActivity(), SensorEventListener
{
    override fun onCreate( savedInstanceState:Bundle? )
    {
        super.onCreate( savedInstanceState )

        sensorManager = getSystemService( SENSOR_SERVICE ) as SensorManager

        setContent()
        {
            LaserScreen()
        }

        val pref:SharedPreferences = getPreferences( MODE_PRIVATE )
        usePercent.value = pref.getBoolean( PREF_USE_PERCENT, usePercent.value )

        val symbol:String = GetSymbol( usePercent.value, this )
        hAngleText.value = "0$symbol"

        hAngle.floatValue = 0f
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

            val rotation:Int = windowManager.defaultDisplay.rotation
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


@Composable
fun CameraView()
{
    val preview = remember { androidx.camera.core.Preview.Builder().build() }
    AndroidView( factory = { ctx -> val previewView = PreviewView( ctx ).apply{ implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                                                                                scaleType = PreviewView.ScaleType.FILL_CENTER }
                                    preview.setSurfaceProvider( previewView.surfaceProvider )
                                    previewView },
                    modifier = Modifier.fillMaxSize().clipToBounds() )

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider:ProcessCameraProvider? by remember { mutableStateOf( null ) }
    LaunchedEffect( Unit )
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance( context )

        cameraProviderFuture.addListener(
        {
            cameraProvider = cameraProviderFuture.get()
            try
            {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle( lifecycleOwner,
                                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                                    preview )
            }
            catch( e:Exception )
            {
                Log.d( "CameraPreview", "Failed to bind camera", e )
            }
        }, ContextCompat.getMainExecutor( context ) )
    }

    DisposableEffect( Unit )
    {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }
}

@Composable
fun DrawAxis( angleR:Double, colour:Color )
{
    val cosAngleR = cos( angleR )
    val sinAngleR = sin( angleR )

    Canvas( modifier = Modifier.fillMaxSize().clipToBounds() )
    {
        val halfLength:Float = size.maxDimension * 2f
        val start = FloatArray( 2 )
        start[ 0 ] = ( -halfLength * cosAngleR ).toFloat() + size.width * 0.5f
        start[ 1 ] = ( -halfLength * sinAngleR ).toFloat() + size.height * 0.5f

        val end = FloatArray( 2 )
        end[ 0 ] = ( halfLength * cosAngleR ).toFloat() + size.width * 0.5f
        end[ 1 ] = ( halfLength * sinAngleR ).toFloat() + size.height * 0.5f

        drawLine( color = colour,
                    start = Offset( start[ 0 ], start[ 1 ] ),
                    end = Offset( end[ 0 ], end[ 1 ] ),
                    strokeWidth = 1.dp.toPx() )
    }

    Canvas( modifier = Modifier.fillMaxSize().clipToBounds() )
    {
        val halfLength:Float = size.maxDimension * 2f
        val start = FloatArray( 2 )
        start[ 0 ] = ( halfLength * sinAngleR ).toFloat() + size.width * 0.5f
        start[ 1 ] = ( -halfLength * cosAngleR ).toFloat() + size.height * 0.5f

        val end = FloatArray( 2 )
        end[ 0 ] = ( -halfLength * sinAngleR ).toFloat() + size.width * 0.5f
        end[ 1 ] = ( halfLength * cosAngleR ).toFloat() + size.height * 0.5f

        drawLine( color = colour,
                    start = Offset( start[ 0 ], start[ 1 ] ),
                    end = Offset( end[ 0 ], end[ 1 ] ),
                    strokeWidth = 1.dp.toPx() )
    }
}

@Composable
fun CameraPermission()
{
    val permission = Manifest.permission.CAMERA
    var hasPermission by remember { mutableStateOf( false ) }
    var showDialog by remember { mutableStateOf( false ) }

    val launcher = rememberLauncherForActivityResult( ActivityResultContracts.RequestPermission() )
    {
        isGranted -> hasPermission = isGranted
                        if( !isGranted )
                            showDialog = true
    }

    LaunchedEffect( Unit ) { launcher.launch( permission ) }

    if( showDialog )
    {
        AlertDialog( onDismissRequest = { showDialog = false },
                        confirmButton = { Button( onClick = { launcher.launch( permission ) } )
                                            {
                                                Text( "Retry", color = Color.White )
                                            } },
                        title = { Text( "Permission Required" ) },
                        text = { Text( "Camera permission is required to use the laser level" ) } )
    }

    if( hasPermission )
    {
        Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center )
        {
            CameraView()

            DrawAxis( 0.0, Color.Red )

            val jcHAngle by hAngle
            DrawAxis( Math.toRadians( jcHAngle.toDouble() ), Color.Green )

            val jcHAngleText by hAngleText
            Text( text = jcHAngleText,
                    modifier = Modifier.align( Alignment.TopStart ).padding( padding ),
                    style = LocalTextStyle.current.copy( shadow = Shadow( color = Color.Black,
                                                                            offset = Offset.Zero,
                                                                            blurRadius = with( LocalDensity.current ) { 8.dp.toPx() } ) ) )
        }
    }
    else
    {
        Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center )
        {
            Text( "Waiting for permission" )
        }
    }
}

@Composable
fun LaserScreen()
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
                CameraPermission()
            }
        }
    }
}

@Preview( showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode" )
@Composable
fun LaserLevelPreview()
{
    LaserScreen()

    hAngle.floatValue = 17.5f
    hAngleText.value = "%.1f%s".format( hAngle.floatValue, "%" )
}