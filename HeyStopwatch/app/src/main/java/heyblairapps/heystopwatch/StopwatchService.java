package heyblairapps.heystopwatch;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.NotificationCompat;
import android.view.Choreographer;

import java.util.ArrayList;

public class StopwatchService extends Service implements Choreographer.FrameCallback
{
	public static StopwatchService				instance;

	public static NotificationCompat.Builder	builder;

	public static boolean						running;
	public static long							startTime;
	public static long							totalTime;
	public static ArrayList< Long >				lapTimeList;

	public static void toggleStart()
	{
		running = !running;
		if( running )
		{
			instance.createNotification();

			startTime = SystemClock.elapsedRealtime();
			Choreographer.getInstance().postFrameCallback( instance );
		}
		else
		{
			Choreographer.getInstance().removeFrameCallback( instance );

			long currTime	= SystemClock.elapsedRealtime();
			totalTime		+= currTime - startTime;
			startTime		= currTime;

			instance.destroyNotification();
		}
	}

	public static void doLap()
	{
		lapTimeList.add( totalTime );
	}
	
	public static void reset()
	{
		running		= false;
		totalTime	= 0;
		
		lapTimeList.clear();
	}


	@Override
	public void onCreate()
	{
		instance	= this;

		running		= false;
		startTime	= 0;
		totalTime	= 0;
		lapTimeList	= new ArrayList< Long >();
	}

	@Override
	public void onDestroy()
	{
		instance	= null;

		running		= false;
		lapTimeList	= null;

		Choreographer.getInstance().removeFrameCallback( this );

		destroyNotification();
	}

	@Override
	public int onStartCommand( Intent intent, int flags, int startId )
	{
		return START_STICKY;
	}

	@Override
	public IBinder onBind( Intent intent )
	{
		return binder;
	}

	public class LocalBinder extends Binder
	{
		StopwatchService getService() { return StopwatchService.this; }
	}

	public void createNotification()
	{
		builder = new NotificationCompat.Builder( this );
        builder.setSmallIcon( R.drawable.stopwatch_notification_icon );
        builder.setContentTitle( getString( R.string.app_name ) );
        builder.setContentText( MainActivity.getTimeString( totalTime ) );
		builder.setOngoing( true );

		Intent notificationIntent = new Intent( this, MainActivity.class );

		PendingIntent pendingIntent = PendingIntent.getActivity( this, 0, notificationIntent, 0 );
		builder.setContentIntent( pendingIntent );
		startForeground( 1, builder.build() );
	}

	public void updateNotification()
	{
		builder.setContentText( MainActivity.getTimeString( totalTime ) );
		NotificationManager notificationManager = ( NotificationManager ) getSystemService( Context.NOTIFICATION_SERVICE );
		notificationManager.notify( 1, builder.build() );
	}

	public void destroyNotification()
	{
		builder = null;
		stopForeground( true );
	}

	public void doFrame( long frameTimeNanos )
	{
		long currTime	= SystemClock.elapsedRealtime();
		totalTime		+= currTime - startTime;
		startTime		= currTime;

		updateNotification();

		if( running )
			Choreographer.getInstance().postFrameCallback( this );
	}


	private final IBinder binder = new LocalBinder();
}