package amist.amisttimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;

public class TimerService extends Service implements Choreographer.FrameCallback
{
	public final String 				NOTIFICATION_CHANNEL_ID = "AMIST Timer";

	public static TimerService			instance;

	public static Notification.Builder	builder;

	public static boolean				running;
	public static long					startTime;
	public static long					totalTime;
	public static long					timeLeft;

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

			instance.stopAlarm();
		}
	}

	public static void setPickedTime( long time )
	{
		pickedTime	= time;

		timeLeft	= Math.max( pickedTime - totalTime, 0 );
	}

	public static Long getPickedTime()
	{
		return pickedTime;
	}

	public static void setVolume( int newVolume )
	{
		volume = newVolume;

		if( instance != null )
			instance.setStreamVolumeIfPlaying();
	}

	public static int getVolume()
	{
		return volume;
	}
	
	public static void reset()
	{
		running		= false;

		totalTime	= 0;
		timeLeft	= 0;

		pickedTime	= 0;
	}


	@Override
	public void onCreate()
	{
		instance		= this;

		running			= false;
		isAlarmPlaying	= false;

		startTime		= 0;
		totalTime		= 0;

		pickedTime		= 0;

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

		if( alarm != null )
		{
			if( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
			{
				AudioAttributes.Builder builder = new AudioAttributes.Builder();
				builder.setUsage( AudioAttributes.USAGE_ALARM );
				AudioAttributes attributes = builder.build();
				alarm.setAudioAttributes( attributes );
			}
			else
			{
				alarm.setStreamType( AudioManager.STREAM_ALARM );
			}
		}
		//*/


		audioManager	= ( AudioManager ) getSystemService( Context.AUDIO_SERVICE );
		maxVolume		= audioManager.getStreamMaxVolume( AudioManager.STREAM_ALARM );

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.P )
			minVolume = audioManager.getStreamMinVolume( AudioManager.STREAM_ALARM );
		else
			minVolume = 0;


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
		instance		= null;

		running			= false;
		isAlarmPlaying	= false;

		Choreographer.getInstance().removeFrameCallback( this );

		destroyNotification();

		stopAlarm();

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
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
			builder = new Notification.Builder( this, NOTIFICATION_CHANNEL_ID );
		else
			builder = new Notification.Builder( this );

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
		
		try
		{
			//when run as an instant app this can crash the app on some devices, catching the
			//exception appears to allow you to still update the notification on these devices
			notificationManager.notify( 1, builder.build() );
		}
		catch( Exception exception )
		{
			Log.i( getString( R.string.app_name ), exception.toString() );
		}
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

			if( !isAlarmPlaying )
			{
				Intent serviceIntent = new Intent( this, MainActivity.class );
				serviceIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP );
				startActivity( serviceIntent );

				defaultVolume = audioManager.getStreamVolume( AudioManager.STREAM_ALARM );
				setStreamVolume( volume );
			}

			volume = audioManager.getStreamVolume( AudioManager.STREAM_ALARM );

			if( alarm != null && !alarm.isPlaying() )
			{
				alarm.play();
				isAlarmPlaying = true;
			}
		}
		
		if( running )
			Choreographer.getInstance().postFrameCallback( this );

		updateNotification();
	}

	public void setStreamVolumeIfPlaying()
	{
		if( isAlarmPlaying )
			setStreamVolume( volume );
	}


	private final IBinder binder = new LocalBinder();

	private static long pickedTime;

	private static Ringtone alarm;
	private boolean isAlarmPlaying;


	private AudioManager audioManager;
	private static int volume;
	private int	defaultVolume;
	private int	maxVolume;
	private int	minVolume;


	private void stopAlarm()
	{
		if( alarm != null )
		{
			alarm.stop();
			isAlarmPlaying = false;

			setStreamVolume( defaultVolume );
		}
	}

	private void setStreamVolume( int volume )
	{
		try
		{
			audioManager.setStreamVolume( AudioManager.STREAM_ALARM, volume, 0 );
		}
		catch( Exception exception )
		{
			Log.i( getString( R.string.app_name ), exception.toString() );
		}
	}
}