package amist.amistspeed

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration
import android.content.res.Resources;
import android.content.SharedPreferences
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.Manifest;
import android.os.Bundle;
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import kotlin.math.*;

class MainActivity : AppCompatActivity()
{
    val LOCATION_PERMISSION_ID:Int          = 1;
    var locationPermissionGranted:Boolean   = false;
    var locationUpdatesRequested:Boolean    = false;


    var hasSpeed:Boolean            = false;
    var previousLocation:Location?  = null;

    lateinit var locationManager:LocationManager;
    var locationListener:LocationListener = object:LocationListener
    {
        override fun onLocationChanged( location:Location)
        {
            var speedMS:Float = location.speed;

            hasSpeed = hasSpeed || ( location.hasSpeed() && speedMS > 0.01f );

            //android can return true for location.hasSpeed but still return 0 for location.speed
            //calculate speed based on position updates, will be less accurate but more useful than 0
            if( !hasSpeed )
            {
                if( previousLocation != null )
                {
                    //location.distanceTo doesn't take into account altitude
                    var distance:Float  = location.distanceTo( previousLocation );
                    var time:Float      = ( location.elapsedRealtimeNanos - previousLocation!!.elapsedRealtimeNanos ) / 1000000000f;
                    speedMS             = distance / time;
                }
            }

            var textSize:Float = floor( min( window.decorView.width / 3.0f, getResources().getDimensionPixelSize( R.dimen.max_speed_text_size ).toFloat() ) );
            speedTF.setTextSize( TypedValue.COMPLEX_UNIT_PX, textSize );

            when( speedUnit )
            {
                SpeedUnit.MPH ->
                {
                    var speedMPH:Float  = min( speedMS * 2.236936f, 999.9f );
                    speedTF.text        = "%.1f".format( speedMPH );

                    if( unitTF.text == "" )
                        unitTF.text = getString( R.string.unit_text_mph );
                }
                SpeedUnit.KPH ->
                {
                    var speedKPH:Float  = min( speedMS * 3.6f, 999.9f );
                    speedTF.text        = "%.1f".format( speedKPH );

                    if( unitTF.text == "" )
                        unitTF.text = getString( R.string.unit_text_kph );
                }
                SpeedUnit.KN ->
                {
                    var speedKn:Float   = min( speedMS * 1.943844f, 999.9f );
                    speedTF.text        = "%.1f".format( speedKn );

                    if( unitTF.text == "" )
                        unitTF.text = getString( R.string.unit_text_kn );
                }
            }

            previousLocation = location;
        }

        override fun onProviderEnabled( provider:String )
        {
            displayMessage( R.string.user_message_getting_location,
                            R.dimen.user_message_size,
                            "provider enabled: " + provider );
        }

        override fun onProviderDisabled( provider:String )
        {
            displayMessage( R.string.user_message_enable_location,
                            R.dimen.user_message_size,
                            "provider disabled: " + provider );
        }

        //deprecated in api 29 but have to keep this here
        override fun onStatusChanged( provider:String,
                                      status:Int,
                                      extras:Bundle ) {}
    };


    enum class SpeedUnit
    {
        MPH,
        KPH,
        KN
    }

    var speedUnit:SpeedUnit = SpeedUnit.MPH;


    lateinit var speedTF:TextView;
    lateinit var unitTF:TextView;


    val PREF_SPEED_UNIT:String  = "speed_unit";
    val PREF_HAS_SPEED:String   = "has_speed";


    override fun onCreate( savedInstanceState:Bundle? )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        speedTF = findViewById( R.id.speedTF );
        unitTF  = findViewById( R.id.unitTF );

        var pref:SharedPreferences  = getPreferences( Context.MODE_PRIVATE );
        speedUnit                   = SpeedUnit.values()[ pref.getInt( PREF_SPEED_UNIT, SpeedUnit.MPH.ordinal ) ];
        hasSpeed                    = pref.getBoolean( PREF_HAS_SPEED, hasSpeed );
    }

    override fun onCreateOptionsMenu( menu:Menu ) : Boolean
    {
        menuInflater.inflate( R.menu.menu, menu );
        return true;
    }

    override fun onStart()
    {
        super.onStart();

        locationPermissionGranted = false;

        if( ContextCompat.checkSelfPermission( applicationContext,
                                               Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
        {
            if( ActivityCompat.shouldShowRequestPermissionRationale( this,
                                                                     Manifest.permission.ACCESS_FINE_LOCATION ) )
            {
                displayMessage( R.string.user_message_require_permission,
                                R.dimen.user_message_size,
                                "show permission message" );
            }
            else
            {
                ActivityCompat.requestPermissions( this,
                                                   arrayOf( Manifest.permission.ACCESS_FINE_LOCATION ),
                                                   LOCATION_PERMISSION_ID );
            }
        }
        else
        {
            locationPermissionGranted = true;
            displayMessage( R.string.user_message_getting_location,
                            R.dimen.user_message_size,
                            "permission already granted" );

            //this call doesn't seem to work on all devices when called in onCreate
            locationManager = getSystemService( Context.LOCATION_SERVICE ) as LocationManager;
            locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                                                    0,
                                                    0f,
                                                    locationListener );
            locationUpdatesRequested = true;
        }
    }

    override fun onConfigurationChanged( config:Configuration )
    {
        super.onConfigurationChanged( config );
    }

    override fun onStop()
    {
        super.onStop();

        if( locationPermissionGranted )
            locationManager.removeUpdates( locationListener );
        
        locationUpdatesRequested = false;
    }

    override fun onDestroy()
    {
        super.onDestroy()

        var prefEditor:SharedPreferences.Editor = getPreferences( Context.MODE_PRIVATE ).edit();
        prefEditor.putInt( PREF_SPEED_UNIT, speedUnit.ordinal );
        prefEditor.putBoolean( PREF_HAS_SPEED, hasSpeed );
        prefEditor.apply();
    }

    override fun onRequestPermissionsResult( requestCode:Int,
                                             permissions:Array< String >,
                                             grantResults:IntArray )
    {
        if( requestCode == LOCATION_PERMISSION_ID && !locationUpdatesRequested )
        {
            locationPermissionGranted = grantResults.isNotEmpty() && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED;
            if( locationPermissionGranted )
            {
                displayMessage( R.string.user_message_getting_location,
                                R.dimen.user_message_size,
                                "permission granted by user" );

                //this call doesn't seem to work on all devices when called in onCreate
                locationManager = getSystemService( Context.LOCATION_SERVICE ) as LocationManager;
                locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                                                        0,
                                                        0f,
                                                        locationListener );
                locationUpdatesRequested = true;
            }
            else
            {
                displayMessage( R.string.user_message_require_permission,
                                R.dimen.user_message_size,
                                "permission denied by user" );
            }
        }
    }

    override fun onOptionsItemSelected( item:MenuItem ) : Boolean
    {
        when( item.itemId )
        {
            R.id.menu_mph ->
            {
                speedUnit = SpeedUnit.MPH;

                if( unitTF.text != "" )
                    unitTF.text = getString( R.string.unit_text_mph );

                return true;
            }
            R.id.menu_kph ->
            {
                speedUnit = SpeedUnit.KPH;

                if( unitTF.text != "" )
                    unitTF.text = getString( R.string.unit_text_kph );

                return true;
            }
            R.id.menu_kn ->
            {
                speedUnit = SpeedUnit.KN;
                
                if( unitTF.text != "" )
                    unitTF.text = getString( R.string.unit_text_kn );

                return true;
            }
            else ->
            {
                var result:Boolean = super.onOptionsItemSelected( item );
                return result;
            }
        }
    }

    fun displayMessage( userMessageId:Int,
                        userMessageSizeId:Int,
                        logMessage:String )
    {
        //the call to getDimensionPixelSize() shouldn't require getResources() but won't compile without it
        //it then complains getResources() can be replaced with Resources but that won't compile either, good job
        speedTF.setTextSize( TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize( userMessageSizeId ).toFloat() );
        speedTF.text = getString( userMessageId );

        unitTF.text = "";

        if( logMessage != "" )
            Log.i( getString( R.string.app_name ), logMessage );
    }
}
