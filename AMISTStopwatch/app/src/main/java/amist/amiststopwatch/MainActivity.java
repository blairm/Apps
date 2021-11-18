package amist.amiststopwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Choreographer.FrameCallback
{
	public final String LAP_TIME_COUNT_ID	= "lapTimeCount";
	public final String LAP_TIME_ID			= "lapTime";
	public final String TOTAL_TIME_ID		= "totalTime";


	public static String getTimeString( Long ms )
	{
		Long s	= ( ms / 1000 ) % 60;
		Long m	= ( ms / ( 1000 * 60 ) ) % 60;
		Long h	= ms / ( 1000 * 60 * 60 );
		ms		-= ( s * 1000 ) + ( m * 1000 * 60 ) + ( h * 1000 * 60 * 60 );

		String result;
		
		if( h < 100 )
			result = String.format( "%02d:%02d:%02d:%03d", h, m, s, ms );
		else
			result = "99:59:59:999";			//limit maximum time displayed

		return result;
	}


	@Override
	public void onClick( View view )
	{
		switch( view.getId() )
		{
			case R.id.buttonStart:
			{
				StopwatchService.toggleStart();
				if( StopwatchService.running )
				{
					buttonStart.setText( R.string.stop_button );
					buttonReset.setText( R.string.lap_button );
				}
				else
				{
					buttonStart.setText( R.string.start_button );
					buttonReset.setText( R.string.reset_button );
				}
				
				if( StopwatchService.running )
					getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
				else
					getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

				break;
			}
			case R.id.buttonReset:
			{
				if( StopwatchService.running )
					StopwatchService.doLap();
				else
					StopwatchService.reset();

				updateList();

				break;
			}
		}
	}

	@Override
	public void doFrame( long frameTimeNanos )
	{
		timeText.setText( getTimeString( StopwatchService.totalTime ) );

		Choreographer.getInstance().postFrameCallback( this );
	}

	@Override
	public void onConfigurationChanged( Configuration config )
	{
		super.onConfigurationChanged( config );
		setContentView( R.layout.activity_main );

		timeText	= ( TextView ) findViewById( R.id.timeText );
		buttonStart	= ( Button ) findViewById( R.id.buttonStart );
		buttonStart.setOnClickListener( this );
		buttonReset	= ( Button ) findViewById( R.id.buttonReset );
		buttonReset.setOnClickListener( this );

		updateActivity();
		
		if( StopwatchService.running )
			getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
		else
			getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
	}


	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		timeText	= ( TextView ) findViewById( R.id.timeText );
		buttonStart = ( Button ) findViewById( R.id.buttonStart );
		buttonStart.setOnClickListener( this );
		buttonReset	= ( Button ) findViewById( R.id.buttonReset );
		buttonReset.setOnClickListener( this );

		//*check for saved lap times and total time, load if found
		SharedPreferences preferences = getPreferences( MODE_PRIVATE );
		if( preferences != null )
		{

			int count = preferences.getInt( LAP_TIME_COUNT_ID, 0 );
			for( int i = 0; i < count; ++i )
			{
				long value = preferences.getLong( LAP_TIME_ID + i, 0 );
				savedLapTimeList.add( value );
			}

			savedTotalTime = preferences.getLong( TOTAL_TIME_ID, 0 );
		}
		//*/
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		updateActivity();

		Choreographer.getInstance().postFrameCallback( this );
		
		if( StopwatchService.running )
			getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
		else
			getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		
		if( !StopwatchService.running )
		{
			if( StopwatchService.totalTime == 0 )
			{
				Intent intent = new Intent( this, StopwatchService.class );
				stopService( intent );
			}
			else
			{
				//*save lap times and total time
				SharedPreferences preferences = getPreferences( MODE_PRIVATE );
				if( preferences != null )
				{
					SharedPreferences.Editor editor = preferences.edit();
					if( editor != null )
					{
						int count = StopwatchService.lapTimeList.size();
						editor.putInt( LAP_TIME_COUNT_ID, count );

						for( int i = 0; i < count; ++i )
							editor.putLong( LAP_TIME_ID + i, StopwatchService.lapTimeList.get( i ) );

						editor.putLong( TOTAL_TIME_ID, StopwatchService.totalTime );

						editor.commit();
					}
				}
				//*/
			}
		}

		Choreographer.getInstance().removeFrameCallback( this );
	}


	private TextView			timeText;
	private Button				buttonStart;
	private Button				buttonReset;

	private ArrayList< Long >	savedLapTimeList	= new ArrayList< Long >();
	private long				savedTotalTime		= 0;

	private void updateActivity()
	{
		if( StopwatchService.instance == null )
		{
			Intent intent = new Intent( this, StopwatchService.class );
			startService( intent );

			for( int i = 0; i < savedLapTimeList.size(); ++i )
				StopwatchService.lapTimeList.add( savedLapTimeList.get( i ) );

			StopwatchService.totalTime = savedTotalTime;
		}

		updateList();

		if( StopwatchService.running )
		{
			buttonStart.setText( R.string.stop_button );
			buttonReset.setText( R.string.lap_button );
		}
		else
		{
			buttonStart.setText( R.string.start_button );
			buttonReset.setText( R.string.reset_button );
		}
	}

	private void updateList()
	{
		int lapCount = StopwatchService.lapTimeList.size();

		ArrayList< String > lapList = new ArrayList< String >();

		for( int i = lapCount - 1; i >= 1; --i )
		{
			Long result = StopwatchService.lapTimeList.get( i ) - StopwatchService.lapTimeList.get( i - 1 );
			lapList.add( getLapString( i + 1, result ) );
		}

		if( lapCount > 0 )
			lapList.add( getLapString( 1, StopwatchService.lapTimeList.get( 0 ) ) );

		ArrayAdapter< String >	lapAdapter	= new ArrayAdapter< String >( this, android.R.layout.simple_list_item_1, lapList );
		ListView	lapListView				= ( ListView ) findViewById( R.id.lapList );
		lapListView.setAdapter( lapAdapter );
	}

	private String getLapString( int lap, Long lapTime )
	{
		String time		= getTimeString(lapTime);
		String result	= getString( R.string.lap_Text ) + " " + lap + ": " + time;
		return result;
	}
}