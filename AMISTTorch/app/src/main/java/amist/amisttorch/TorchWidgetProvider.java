package amist.amisttorch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class TorchWidgetProvider extends AppWidgetProvider
{
	public static String ACTION_CLICK_WIDGET = "updateWidgetFromProvider";

	@Override
	public void onReceive( Context context, Intent intent )
	{
		super.onReceive( context, intent );

		String action = intent.getAction();
		if( action.equals( ACTION_CLICK_WIDGET ) )
		{
			RemoteViews views = new RemoteViews( context.getPackageName(), R.layout.widget );

			if( !TorchService.torchOn )
				views.setInt( R.id.widget_torch_image, "setAlpha", 255 );
			else
				views.setInt( R.id.widget_torch_image, "setAlpha", 100 );
			
			Intent torchIntent = new Intent( context, TorchService.class );
			context.startService( torchIntent );

			Intent clickIntent = new Intent( context, TorchWidgetProvider.class );
			clickIntent.setAction( ACTION_CLICK_WIDGET );
			PendingIntent pendingIntent = PendingIntent.getBroadcast( context, 0, clickIntent, PendingIntent.FLAG_IMMUTABLE );
			views.setOnClickPendingIntent( R.id.widget_torch_switch, pendingIntent );

			ComponentName componentName = new ComponentName( context, TorchWidgetProvider.class );
			AppWidgetManager.getInstance( context ).updateAppWidget( componentName, views );
		}
		else if( action.equals( TorchService.ACTION_UPDATE_WIDGET ) )
		{
			RemoteViews views = new RemoteViews( context.getPackageName(), R.layout.widget );
			
			if( TorchService.torchOn )
				views.setInt( R.id.widget_torch_image, "setAlpha", 255 );
			else
				views.setInt( R.id.widget_torch_image, "setAlpha", 100 );

			Intent clickIntent = new Intent( context, TorchWidgetProvider.class );
			clickIntent.setAction( ACTION_CLICK_WIDGET );
			PendingIntent pendingIntent = PendingIntent.getBroadcast( context, 0, clickIntent, PendingIntent.FLAG_IMMUTABLE );
			views.setOnClickPendingIntent( R.id.widget_torch_switch, pendingIntent );

			ComponentName componentName = new ComponentName( context, TorchWidgetProvider.class );
			AppWidgetManager.getInstance( context ).updateAppWidget( componentName, views );
		}
	}

	@Override
	public void onUpdate( Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds )
	{
		super.onUpdate( context, appWidgetManager, appWidgetIds );

		for( int i = 0; i < appWidgetIds.length; ++i )
		{
			int appWidgetId = appWidgetIds[ i ];

			RemoteViews views = new RemoteViews( context.getPackageName(), R.layout.widget );

			if( TorchService.torchOn )
				views.setInt( R.id.widget_torch_image, "setAlpha", 255 );
			else
				views.setInt( R.id.widget_torch_image, "setAlpha", 100 );
			
			Intent intent = new Intent( context, TorchWidgetProvider.class );
			intent.setAction( ACTION_CLICK_WIDGET );
			PendingIntent pendingIntent = PendingIntent.getBroadcast( context, 0, intent, PendingIntent.FLAG_IMMUTABLE );
			views.setOnClickPendingIntent( R.id.widget_torch_switch, pendingIntent );

			appWidgetManager.updateAppWidget( appWidgetId, views );
		}
	}
}