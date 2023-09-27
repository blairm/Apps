package amist.amisttorch;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity
{
    public ImageView torchImage;

    public BroadcastReceiver notificationReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {
			if( TorchService.torchOn )
				torchImage.setImageAlpha( 255 );
			else
				torchImage.setImageAlpha( 100 );
        }
    };

    public boolean onTouchEvent( MotionEvent event )
    {
        int eventAction = event.getAction();

        if( eventAction == MotionEvent.ACTION_UP )
        {
			//doing this here makes the torch image go on or off
			//quicker than waiting for the broadcast receiver
			if( !TorchService.torchOn )
				torchImage.setImageAlpha( 255 );
			else
				torchImage.setImageAlpha( 100 );

			Intent intent = new Intent( this, TorchService.class );
			startService( intent );
        }

        return true;
    }


    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        torchImage = ( ImageView ) findViewById( R.id.imageView );
    }

	@SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
	protected void onResume()
	{
		super.onResume();

		if( TorchService.torchOn )
			torchImage.setImageAlpha( 255 );
		else
			torchImage.setImageAlpha( 100 );

		IntentFilter filter = new IntentFilter();
        filter.addAction( TorchService.ACTION_UPDATE_ACTIVITY );

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE )
            registerReceiver( notificationReceiver, filter, RECEIVER_NOT_EXPORTED );
        else
            registerReceiver( notificationReceiver, filter );
	}

	@Override
    protected void onPause()
	{
        super.onPause();

        unregisterReceiver( notificationReceiver );
    }
}