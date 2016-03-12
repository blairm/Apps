package heyblairapps.heytorch;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;

public class TorchService extends Service
{
	public static String	ACTION_UPDATE_ACTIVITY	= "updateActivityFromService";
	public static String	ACTION_UPDATE_WIDGET	= "updateWidgetFromService";

	public static boolean	torchOn					= false;


	public Camera camera;

	@Override
	public void onCreate()
	{
		camera = Camera.open();
	}

	@Override
	public void onDestroy()
	{
		setTorchOff();

		if( camera != null )
		{
			camera.release();
			camera = null;
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

		if( camera != null )
		{
			camera.stopPreview();
			Parameters camParams = camera.getParameters();
			camParams.setFlashMode( Parameters.FLASH_MODE_OFF );
			camera.setParameters( camParams );
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

		if( camera != null )
		{
			Parameters camParams = camera.getParameters();
			camParams.setFlashMode( Parameters.FLASH_MODE_TORCH );
			camera.setParameters(camParams);
			camera.startPreview();
		}

		Intent intent = new Intent( this, TorchWidgetProvider.class );
		intent.setAction( ACTION_UPDATE_WIDGET );
		sendBroadcast( intent );

		NotificationCompat.Builder builder = new NotificationCompat.Builder( this );
        builder.setSmallIcon( R.drawable.torch_notification_icon );
        builder.setContentTitle( getString( R.string.app_name ) );
        builder.setContentText( getString( R.string.notification_text ) );
		builder.setOngoing( true );

		Intent notificationIntent = new Intent( this, TorchService.class );

		PendingIntent pendingIntent = PendingIntent.getService( this, 0, notificationIntent, 0 );
		builder.setContentIntent( pendingIntent );
		startForeground( 1, builder.build() );
	}


	private final IBinder binder = new LocalBinder();
}