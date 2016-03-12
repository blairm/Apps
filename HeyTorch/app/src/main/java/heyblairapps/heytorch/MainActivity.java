package heyblairapps.heytorch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MotionEvent;
import android.widget.ImageView;

public class MainActivity extends ActionBarActivity
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
        int eventaction = event.getAction();

        if( eventaction == MotionEvent.ACTION_UP )
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
        registerReceiver( notificationReceiver, filter );
	}

	@Override
    protected void onPause()
	{
        super.onPause();

		unregisterReceiver( notificationReceiver );
    }
}