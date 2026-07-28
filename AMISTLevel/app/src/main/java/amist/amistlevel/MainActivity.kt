package amist.amistlevel

import amist.amistlevel.ui.theme.AMISTLevelTheme
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

class MainActivity : ComponentActivity()
{
    override fun onCreate( savedInstanceState:Bundle? )
    {
        super.onCreate( savedInstanceState )

        setContent()
        {
            SelectScreen()
        }

        val pref:SharedPreferences = getPreferences( MODE_PRIVATE )
        usePercent.value = pref.getBoolean( PREF_USE_PERCENT, usePercent.value )
    }


    override fun onPause()
    {
        super.onPause()

        getPreferences( MODE_PRIVATE ).edit{
            putBoolean( PREF_USE_PERCENT, usePercent.value )
        }
    }
}


@Composable
fun SelectButtons( isPortrait:Boolean )
{
    val context = LocalContext.current

    var buttonModifier = Modifier.fillMaxHeight( 0.35f ).aspectRatio( 1f, true )
    if( isPortrait )
        buttonModifier = Modifier.fillMaxWidth( 0.35f ).aspectRatio( 1f, false )

    Button( onClick = { context.startActivity( Intent( context, BubbleActivity::class.java ) ) } )
    {
        Column( horizontalAlignment = Alignment.CenterHorizontally )
        {
            Icon( imageVector = ImageVector.vectorResource( R.drawable.iconlevel ),
                    contentDescription = stringResource( R.string.BubbleLevel ),
                    modifier = buttonModifier )
            Text( stringResource( R.string.BubbleLevel ) )
        }
    }

    Spacer( modifier = Modifier.size( 50.dp ) )

    Button( onClick = { context.startActivity( Intent( context, LaserActivity::class.java ) ) } )
    {
        Column( horizontalAlignment = Alignment.CenterHorizontally )
        {
            Icon( imageVector = ImageVector.vectorResource( R.drawable.iconlaser ),
                    contentDescription = stringResource( R.string.LaserLevel ),
                    modifier = buttonModifier )
            Text( stringResource( R.string.LaserLevel ) )
        }
    }
}

@Composable
fun SelectScreen()
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

                if( LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT )
                {
                    Column( modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally )
                    {
                        SelectButtons( true )
                    }
                }
                else
                {
                    Row( modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center )
                    {
                        SelectButtons( false )
                    }
                }
            }
        }
    }
}

@Preview( showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode" )
@Composable
fun SelectPreview()
{
    SelectScreen()
}