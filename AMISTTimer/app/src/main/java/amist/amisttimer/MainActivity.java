package amist.amisttimer;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Choreographer.FrameCallback
{
	public static String getTimeString( Long ms )
	{
		Long s	= ( ms / 1000 ) % 60;
		Long m	= ( ms / ( 1000 * 60 ) ) % 60;
		Long h	= ms / ( 1000 * 60 * 60 );

		String result;
		
		if( h < 100 )
			result = String.format( "%02d:%02d:%02d", h, m, s );
		else
			result = "99:59:59";			//limit maximum time displayed

		return result;
	}


	public final String RECENT_TIMERS_COUNT_ID	= "recentTimersCount";
	public final String RECENT_TIMER_ID			= "recentTimer";


	@Override
	public void onClick( View view )
	{
		switch( view.getId() )
		{
			case R.id.buttonStart:
			{
				TimerService.toggleStart();
				if( TimerService.running )
				{
					buttonStart.setText( R.string.stop_button );
					buttonSetTime.setText( R.string.reset_button );

					if( pickedTime == TimerService.timeLeft )
					{
						int indexRecentTimerList = -1;

						for( int i = 0; i < recentTimerList.size(); ++i )
						{
							if( pickedTime.longValue() == recentTimerList.get( i ).longValue() )
							{
								indexRecentTimerList = i;
								break;
							}
						}

						if( indexRecentTimerList >= 0 )
						{
							for( int i = indexRecentTimerList - 1; i >= 0; --i )
							{
								Long time = recentTimerList.get( i );
								recentTimerList.set( i + 1, time );
							}

							recentTimerList.set( 0, pickedTime );
						}
						else
						{
							recentTimerList.add( 0, pickedTime );

							if( recentTimerList.size() > recentTimerListMaxCount )
								recentTimerList.remove( recentTimerListMaxCount );
						}

						updateList();
					}
				}
				else
				{
					buttonStart.setText( R.string.start_button );

					//*if timer has finished, automatically reset timer
					if( TimerService.timeLeft <= 0 )
					{
						buttonSetTime.setText( R.string.set_time_button );

						TimerService.reset();
						pickedTime = ( long ) 0;

						hourPicker.setValue( 0 );
						minutePicker.setValue( 0 );
						secondPicker.setValue( 0 );
					}
					//*/
				}

				buttonClear.setEnabled( true );

				break;
			}
			case R.id.buttonSetTime:
			{
				if( !TimerService.running )
				{
					if( TimerService.totalTime == 0 )
					{
						AlertDialog.Builder builder = new AlertDialog.Builder( this, R.style.AlertDialogTheme );
						builder.setView( dialogTimePickerView );
						builder.setTitle( R.string.time_picker_title );

						builder.setPositiveButton( R.string.positive_text, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick( DialogInterface dialog, int value )
							{
								pickedTime = ( ( long ) secondPicker.getValue() ) * 1000;
								pickedTime += ( ( long ) minutePicker.getValue() ) * 1000 * 60;
								pickedTime += ( ( long ) hourPicker.getValue() ) * 1000 * 60 * 60;
								TimerService.reset();
								TimerService.setPickedTime( pickedTime );
							}
						} );

						builder.setNegativeButton( R.string.negative_text, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick( DialogInterface dialog, int value )
							{
								hourPicker.setValue( ( int ) ( pickedTime / ( 1000 * 60 * 60 ) ) );
								minutePicker.setValue( ( int ) ( ( pickedTime / ( 1000 * 60 ) ) % 60 ) );
								secondPicker.setValue( ( int ) ( ( pickedTime / 1000 ) % 60 ) );
							}
						} );

						builder.setOnDismissListener( new DialogInterface.OnDismissListener()
						{
							@Override
							public void onDismiss( DialogInterface dialog )
							{
								if( dialogTimePickerView != null )
								{
									ViewGroup dialogParent = ( ViewGroup ) dialogTimePickerView.getParent();

									if( dialogParent != null )
										dialogParent.removeView( dialogTimePickerView );
								}
							}
						} );

						builder.show();
					}
					else
					{
						buttonSetTime.setText( R.string.set_time_button );

						TimerService.reset();
						pickedTime = ( long ) 0;

						hourPicker.setValue( 0 );
						minutePicker.setValue( 0 );
						secondPicker.setValue( 0 );
					}
				}

				break;
			}
			case R.id.buttonClear:
			{
				recentTimerList.clear();
				updateList();
				buttonClear.setEnabled( false );

				break;
			}
		}
	}

	@Override
	public void doFrame( long frameTimeNanos )
	{
		timeText.setText( getTimeString( TimerService.timeLeft ) );

		if( ( pickedTime != 0 && TimerService.timeLeft > 0 ) || TimerService.running )
			buttonStart.setEnabled( true );
		else
			buttonStart.setEnabled( false );

		buttonSetTime.setEnabled( !TimerService.running );

		TimerService.loopAlarm();

		Choreographer.getInstance().postFrameCallback( this );
	}

	@Override
	public void onConfigurationChanged( Configuration config )
	{
		super.onConfigurationChanged( config );
		setupActivityLayout();

		updateActivity();
	}


	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setupActivityLayout();

		//*check for saved timers, load if there are any
		SharedPreferences preferences = getPreferences( MODE_PRIVATE );

		if( preferences != null )
		{
			int count = preferences.getInt( RECENT_TIMERS_COUNT_ID, 0 );

			for( int i = 0; i < count; ++i )
			{
				long value = preferences.getLong( RECENT_TIMER_ID + i, 0 );
				recentTimerList.add( value );
			}
		}
		//*/
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		updateActivity();

		Choreographer.getInstance().postFrameCallback( this );
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		
		if( !TimerService.running && TimerService.totalTime == 0 )
		{
			Intent intent = new Intent( this, TimerService.class );
			stopService( intent );
		}

		Choreographer.getInstance().removeFrameCallback( this );

		//*save recent timers if there any
		SharedPreferences preferences = getPreferences( MODE_PRIVATE );

		if( preferences != null )
		{
			SharedPreferences.Editor editor = preferences.edit();

			if( editor != null )
			{
				int count = recentTimerList.size();
				editor.putInt( RECENT_TIMERS_COUNT_ID, count );

				for( int i = 0; i < count; ++i )
					editor.putLong( RECENT_TIMER_ID + i, recentTimerList.get( i ) );

				editor.commit();
			}
		}
		//*/
	}


	private TextView			timeText;
	private Button				buttonStart;
	private Button				buttonSetTime;
	private Button				buttonClear;

	private View				dialogTimePickerView;
	private NumberPicker		hourPicker;
	private NumberPicker		minutePicker;
	private NumberPicker		secondPicker;

	private Long				pickedTime				= ( long ) 0;
	private ArrayList< Long >	recentTimerList			= new ArrayList< Long >();
	private int					recentTimerListMaxCount	= 15;

	private void setupActivityLayout()
	{
		setContentView( R.layout.activity_main );

		timeText		= ( TextView ) findViewById( R.id.timeText );
		buttonStart		= ( Button ) findViewById( R.id.buttonStart );
		buttonStart.setOnClickListener( this );
		buttonSetTime	= ( Button ) findViewById( R.id.buttonSetTime );
		buttonSetTime.setOnClickListener( this );
		buttonClear		= ( Button ) findViewById( R.id.buttonClear );
		buttonClear.setOnClickListener( this );

		LayoutInflater inflater	= getLayoutInflater();
		dialogTimePickerView	= inflater.inflate( R.layout.dialog_time_picker, null );

		hourPicker				= ( NumberPicker ) dialogTimePickerView.findViewById( R.id.hourPicker );
		minutePicker			= ( NumberPicker ) dialogTimePickerView.findViewById( R.id.minutePicker );
		secondPicker			= ( NumberPicker ) dialogTimePickerView.findViewById( R.id.secondPicker );

		String numberList[] = new String[ 100 ];
		for( int i = 0; i < numberList.length; ++i )
			numberList[ i ] = String.format( "%02d", i );
		hourPicker.setDisplayedValues( numberList );
		hourPicker.setMaxValue( 99 );
		hourPicker.setMinValue( 0 );
		hourPicker.setDescendantFocusability( NumberPicker.FOCUS_BLOCK_DESCENDANTS );
		setNumberPickerDividerColor( hourPicker, getResources().getColor( R.color.numberPickerDividerColour ) );

		numberList = new String[ 60 ];
		for( int i = 0; i < numberList.length; ++i )
			numberList[ i ] = String.format( "%02d", i );
		minutePicker.setDisplayedValues( numberList );
		minutePicker.setMaxValue( 59 );
		minutePicker.setMinValue( 0 );
		minutePicker.setDescendantFocusability( NumberPicker.FOCUS_BLOCK_DESCENDANTS );
		setNumberPickerDividerColor( minutePicker, getResources().getColor( R.color.numberPickerDividerColour ) );
		secondPicker.setDisplayedValues( numberList );
		secondPicker.setMaxValue( 59 );
		secondPicker.setMinValue( 0 );
		secondPicker.setDescendantFocusability( NumberPicker.FOCUS_BLOCK_DESCENDANTS );
		setNumberPickerDividerColor( secondPicker, getResources().getColor( R.color.numberPickerDividerColour ) );
	}

	private void updateActivity()
	{
		if( TimerService.instance == null )
		{
			Intent intent = new Intent( this, TimerService.class );
			startService( intent );
		}

		timeText.setText( getTimeString( TimerService.timeLeft ) );

		updateList();

		if( TimerService.running )
		{
			buttonStart.setText( R.string.stop_button );
			buttonSetTime.setText( R.string.reset_button );
		}
		else
		{
			buttonStart.setText( R.string.start_button );
			buttonSetTime.setText( R.string.set_time_button );
		}

		buttonClear.setEnabled( recentTimerList.size() > 0 );
	}

	private void updateList()
	{
		ArrayList< String > timeList = new ArrayList< String >();

		for( int i = 0; i < recentTimerList.size(); ++i )
		{
			Long time = recentTimerList.get( i );
			timeList.add( getTimeString( time ) );
		}

		ArrayAdapter< String > listAdapter	= new ArrayAdapter< String >( this, android.R.layout.simple_list_item_1, timeList );
		ListView timeListView				= ( ListView ) findViewById( R.id.timeList );
		timeListView.setAdapter( listAdapter );
		timeListView.setOnItemClickListener( new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick( AdapterView< ? > adapter, View view, int position, long id )
			{
				String timeString = ( String ) adapter.getItemAtPosition( position );
				long h = Long.parseLong( timeString.substring( 0, 2 ) );
				long m = Long.parseLong( timeString.substring( 3, 5 ) );
				long s = Long.parseLong( timeString.substring( 6, 8 ) );
				pickedTime = s * 1000;
				pickedTime += m * 1000 * 60;
				pickedTime += h * 1000 * 60 * 60;

				TimerService.reset();
				TimerService.setPickedTime( pickedTime );
			}
		} );
	}

	//@HACK uses private Android API, fail silently if it doesn't work
	private void setNumberPickerDividerColor( NumberPicker picker, int colour )
	{
		try
		{
			Field[] pickerFields = NumberPicker.class.getDeclaredFields();

			for( Field pickerField : pickerFields )
			{
				if( pickerField.getName().equals( "mSelectionDivider" ) )
				{
					pickerField.setAccessible( true );

					ColorDrawable colourDrawable = new ColorDrawable( colour );
					pickerField.set( picker, colourDrawable );

					break;
				}
			}
		}
		catch( Exception e ) {}
	}
	//*/
}