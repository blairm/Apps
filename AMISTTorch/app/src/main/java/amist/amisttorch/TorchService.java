package amist.amisttorch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class TorchService extends Service
{
	public final String 	NOTIFICATION_CHANNEL_ID = "AMIST Torch";


	public static String	ACTION_UPDATE_ACTIVITY	= "updateActivityFromService";
	public static String	ACTION_UPDATE_WIDGET	= "updateWidgetFromService";

	public static boolean	torchOn					= false;


	@Override
	public void onCreate()
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

	@Override
	public void onDestroy()
	{
		setTorchOff();
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

		Intent activityIntent = new Intent();
		activityIntent.setPackage( getPackageName() );
		activityIntent.setAction( ACTION_UPDATE_ACTIVITY );
		sendBroadcast( activityIntent );

		Intent widgetIntent = new Intent( this, TorchWidgetProvider.class );
		widgetIntent.setAction( ACTION_UPDATE_WIDGET );
		sendBroadcast( widgetIntent );

		stopForeground( STOP_FOREGROUND_REMOVE );
	}

	public void setTorchOn()
	{
		torchOn = true;

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

		Intent intent = new Intent( this, TorchWidgetProvider.class );
		intent.setAction( ACTION_UPDATE_WIDGET );
		sendBroadcast( intent );

		Notification.Builder builder = new Notification.Builder( this, NOTIFICATION_CHANNEL_ID );
        builder.setSmallIcon( R.drawable.torch_notification_icon );
        builder.setContentTitle( getString( R.string.app_name ) );
        builder.setContentText( getString( R.string.notification_text ) );
		builder.setOngoing( true );

		Intent notificationIntent = new Intent( this, TorchService.class );

		PendingIntent pendingIntent = PendingIntent.getForegroundService( this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE );

		builder.setContentIntent( pendingIntent );
		startForeground( 1, builder.build() );
	}


	private final IBinder binder = new LocalBinder();
}