package heyblairapps.heytimer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.NotificationCompat;
import android.view.Choreographer;

public class TimerService extends Service implements Choreographer.FrameCallback
{
	public static TimerService					instance;

	public static NotificationCompat.Builder	builder;

	public static boolean						running;
	public static long							startTime;
	public static long							totalTime;
	public static long							timeLeft;

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

			timeLeft		= Math.max( pickedTime - totalTime, 0 );

			instance.destroyNotification();

			if( alarm != null )
				alarm.stop();
		}
	}

	public static void setPickedTime( long time )
	{
		pickedTime	= time;

		timeLeft	= Math.max( pickedTime - totalTime, 0 );
	}
	
	public static void reset()
	{
		running		= false;
		totalTime	= 0;
		timeLeft	= 0;

		pickedTime	= 0;
	}

	public static void loopAlarm()
	{
		if( running && timeLeft <= 0 )
		{
			if( alarm != null && !alarm.isPlaying() )
				alarm.play();
		}
	}


	@Override
	public void onCreate()
	{
		instance	= this;

		running		= false;
		startTime	= 0;
		totalTime	= 0;

		//*get default alarm sound, if null, get ringtone sound, if null, cry
		Uri alarmURI = RingtoneManager.getDefaultUri( RingtoneManager.TYPE_ALARM );

		if( alarmURI != null )
			alarm = RingtoneManager.getRingtone( getApplicationContext(), alarmURI );

		if( alarm == null )
		{
			alarmURI = RingtoneManager.getDefaultUri( RingtoneManager.TYPE_RINGTONE );

			if( alarmURI != null )
				alarm = RingtoneManager.getRingtone( getApplicationContext(), alarmURI );
		}
		//*/
	}

	@Override
	public void onDestroy()
	{
		instance	= null;

		running		= false;

		Choreographer.getInstance().removeFrameCallback( this );

		destroyNotification();

		if( alarm != null )
			alarm.stop();

		alarm = null;
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
		TimerService getService() { return TimerService.this; }
	}

	public void createNotification()
	{
		builder = new NotificationCompat.Builder( this );
        builder.setSmallIcon( R.drawable.timer_notification_icon );
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
		builder.setContentText( MainActivity.getTimeString( timeLeft ) );
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

		timeLeft		= pickedTime - totalTime;

		if( timeLeft <= 0 )
		{
			timeLeft = 0;

			Intent serviceIntent = new Intent( this, MainActivity.class );
			serviceIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP );
			startActivity( serviceIntent );

			if( alarm != null )
				alarm.play();
		}
		else if( running )
		{
			Choreographer.getInstance().postFrameCallback( this );
		}

		updateNotification();
	}


	private final IBinder binder = new LocalBinder();

	private static long pickedTime;

	private static Ringtone alarm;
}