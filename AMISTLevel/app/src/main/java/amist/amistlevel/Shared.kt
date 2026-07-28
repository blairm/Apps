package amist.amistlevel

import android.content.Context
import android.content.res.Configuration
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.lang.Math.clamp
import kotlin.math.asin
import kotlin.math.tan


const val PREF_USE_PERCENT:String  = "use_percent"
var usePercent = mutableStateOf( false )

val padding:Dp = 10.dp

val hAngleText = mutableStateOf( "" )
val vAngleText = mutableStateOf( "" )
val hAngle = mutableFloatStateOf( 0f )
val vAngle = mutableFloatStateOf( 0f )


fun SetAngleValues( accValues:FloatArray,
                    magValues:FloatArray,
                    rotation:Int,
                    context:Context )
{
    val rotationMatrix = FloatArray( 9 )
    SensorManager.getRotationMatrix( rotationMatrix,
                                        null,
                                        accValues,
                                        magValues )


    val angles = DoubleArray( 2 )
    angles[ 0 ] = Math.toDegrees( asin( rotationMatrix[ 6 ].toDouble() ) )
    angles[ 1 ] = Math.toDegrees( asin( -rotationMatrix[ 7 ].toDouble() ) )

    when( rotation )
    {
        Surface.ROTATION_0 -> {}
        Surface.ROTATION_90 ->
        {
            val swap:Double = angles[ 0 ]
            angles[ 0 ] = angles[ 1 ]
            angles[ 1 ] = -swap
        }
        Surface.ROTATION_180 ->
        {
            angles[ 0 ] *= -1
            angles[ 1 ] *= -1
        }
        Surface.ROTATION_270 ->
        {
            val swap:Double = angles[ 0 ]
            angles[ 0 ] = -angles[ 1 ]
            angles[ 1 ] = swap
        }
    }

    hAngle.floatValue = angles[ 0 ].toFloat()
    vAngle.floatValue = angles[ 1 ].toFloat()

    if( usePercent.value )
    {
        val maxValue = 9999999.9
        angles[ 0 ] = clamp( tan( Math.toRadians( angles[ 0 ] ) ) * 100.0, -maxValue, maxValue )
        angles[ 1 ] = clamp( tan( Math.toRadians( angles[ 1 ] ) ) * 100.0, -maxValue, maxValue )
    }

    val symbol:String = GetSymbol( usePercent.value, context )
    hAngleText.value = "%.1f%s".format( angles[ 0 ], symbol )
    vAngleText.value = "%.1f%s".format( angles[ 1 ], symbol )
}

fun GetSymbol( usePercent:Boolean, context:Context ):String
{
    var result:String = context.getString( R.string.SymbolDegrees )
    if( usePercent )
        result = context.getString( R.string.SymbolPercent )

    return result
}

fun IsTablet( config:Configuration ):Boolean
{
    var result = false
    if( config.smallestScreenWidthDp >= 600 )
        result = true

    return result
}


@OptIn( ExperimentalMaterial3Api::class )
@Composable
fun AppBar()
{
    TopAppBar( title = { Text( stringResource( R.string.app_name ) ) },
                actions =
                {
                    var expanded by remember { mutableStateOf( false ) }
                    IconButton( onClick = { expanded = !expanded } )
                    {
                        Icon( Icons.Default.MoreVert, stringResource( R.string.OverflowContentDescription ) )
                    }
                    DropdownMenu( expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background( colorResource( R.color.ic_launcher_background ) ) )
                    {
                        var jcUsePercent by usePercent
                        DropdownMenuItem( text = { Text( stringResource( R.string.OverflowDegrees ) ) },
                                            onClick =
                                            {
                                                jcUsePercent = false
                                                expanded = false
                                            } )
                        DropdownMenuItem( text = { Text( stringResource( R.string.OverflowPercent ) ) },
                                            onClick =
                                            {
                                                jcUsePercent = true
                                                expanded = false
                                            } )
                    }
                }
    )
}