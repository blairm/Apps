package amist.amisttorch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class TorchService extends Service
{
	public final String 	NOTIFICATION_CHANNEL_ID = "AMIST Torch";


	public static String	ACTION_UPDATE_ACTIVITY	= "updateActivityFromService";
	public static String	ACTION_UPDATE_WIDGET	= "updateWidgetFromService";

	public static boolean	torchOn					= false;


	public Camera camera;

	@Override
	public void onCreate()
	{
		if( android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M )
			camera = Camera.open();

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
		{
			NotificationChannel notificationChannel = new NotificationChannel( NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_LOW );
			notificationChannel.enableLights( false );
			notificationChannel.enableVibration( false );

			NotificationManager notificationManager = ( NotificationManager ) getSystemService( Context.NOTIFICATION_SERVICE );

			try
			{
				notificationManager.createNotificationChannel( notificationChannel );
			}
			catch( Exception exception )
			{
				Log.i( getString( R.string.app_name ), exception.toString() );
			}
		}
	}

	@Override
	public void onDestroy()
	{
		setTorchOff();

		if( android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M )
		{
			if( camera != null )
			{
				camera.release();
				camera = null;
			}
		}
	}

	@Override
	public int onStartCommand( Intent intent, int flags, int startId )
	{
		if( torchOn )
			stopSelf();
		else
			setTorchOn();

		return START_STICKY;
	}

	@Override
	public IBinder onBind( Intent intent )
	{
		return binder;
	}

	public class LocalBinder extends Binder
	{
		TorchService getService() { return TorchService.this; }
	}

	public void setTorchOff()
	{
		torchOn = false;

		if( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M )
		{
			try
			{
				CameraManager cameraManager = ( CameraManager ) getSystemService( Context.CAMERA_SERVICE );
				String[] idList = cameraManager.getCameraIdList();
				cameraManager.setTorchMode( idList[0], false );
			}
			catch( Exception exception )
			{
				Log.e( getString( R.string.app_name ), exception.getMessage() );
			}
		}
		else
		{
			if( camera != null )
			{
				camera.stopPreview();
				Parameters camParams = camera.getParameters();
				camParams.setFlashMode( Parameters.FLASH_MODE_OFF );
				camera.setParameters( camParams );
			}
		}

		Intent activityIntent = new Intent();
		activityIntent.setPackage( getPackageName() );
		activityIntent.setAction( ACTION_UPDATE_ACTIVITY );
		sendBroadcast( activityIntent );

		Intent widgetIntent = new Intent( this, TorchWidgetProvider.class );
		widgetIntent.setAction( ACTION_UPDATE_WIDGET );
		sendBroadcast( widgetIntent );

		stopForeground( true );
	}

	public void setTorchOn()
	{
		torchOn = true;

		if( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M )
		{
			try
			{
				CameraManager cameraManager = ( CameraManager ) getSystemService( Context.CAMERA_SERVICE );
				String[] idList = cameraManager.getCameraIdList();
				cameraManager.setTorchMode( idList[0], true );
			}
			catch( Exception exception )
			{
				Log.e( getString( R.string.app_name ), exception.getMessage() );
			}
		}
		else
		{
			if( camera != null )
			{
				Parameters camParams = camera.getParameters();
				camParams.setFlashMode( Parameters.FLASH_MODE_TORCH );
				camera.setParameters(camParams);
				camera.startPreview();
			}
		}

		Intent intent = new Intent( this, TorchWidgetProvider.class );
		intent.setAction( ACTION_UPDATE_WIDGET );
		sendBroadcast( intent );

		Notification.Builder builder;
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
			builder = new Notification.Builder( this, NOTIFICATION_CHANNEL_ID );
		else
			builder = new Notification.Builder( this );

        builder.setSmallIcon( R.drawable.torch_notification_icon );
        builder.setContentTitle( getString( R.string.app_name ) );
        builder.setContentText( getString( R.string.notification_text ) );
		builder.setOngoing( true );

		Intent notificationIntent = new Intent( this, TorchService.class );

		PendingIntent pendingIntent;

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
			pendingIntent = PendingIntent.getForegroundService( this, 0, notificationIntent, 0 );
		else
			pendingIntent = PendingIntent.getService( this, 0, notificationIntent, 0 );

		builder.setContentIntent( pendingIntent );
		startForeground( 1, builder.build() );
	}


	private final IBinder binder = new LocalBinder();
}