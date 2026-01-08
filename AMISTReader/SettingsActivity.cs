using Android.Content;
using Android.OS;
using Android.Util;
using Android.Views;
using AndroidX.Activity;
using AndroidX.AppCompat.App;
using AndroidX.Core.View;
using AndroidX.DrawerLayout.Widget;

namespace AMISTReader
{
    [ Activity( Label = "@string/Settings" ) ]
    public class SettingsActivity : AppCompatActivity
    {
        protected override void OnCreate( Bundle? savedInstanceState )
        {
            base.OnCreate( savedInstanceState );
            SetContentView( Resource.Layout.ActivitySettings );

            OnBackCallback callback = new OnBackCallback( ReturnToPrevActivity, true );
            OnBackPressedDispatcher.AddCallback( this, callback );

            AndroidX.AppCompat.Widget.Toolbar? toolbar = FindViewById< AndroidX.AppCompat.Widget.Toolbar >( Resource.Id.Toolbar );
            SetSupportActionBar( toolbar );
            SupportActionBar?.SetDisplayHomeAsUpEnabled( true );
            SupportActionBar?.SetHomeButtonEnabled( true );

            settings.Load( Intent, Resources );

            maxItemsPerFeed = FindViewById< LinearLayout >( Resource.Id.MaxItemsPerFeed );
            maxItemsPerFeed?.Click += MaxItemsPerFeedClicked;

            maxItemsPerFeedCount = FindViewById< TextView >( Resource.Id.MaxItemsPerFeedCount );
            maxItemsPerFeedCount?.Text = settings.maxItemsPerFeed.ToString();

            openLinkAutomaticallySwitch = FindViewById< Switch >( Resource.Id.SettingsOpenLinkAutomatically );
            openLinkAutomaticallySwitch?.Checked = settings.openLinkAutomatically;
            openLinkAutomaticallySwitch?.CheckedChange += OpenLinkAutomaticallyChecked;

            enableJSSwitch = FindViewById< Switch >( Resource.Id.SettingsEnableJS );
            enableJSSwitch?.Checked = settings.enableJS;
            enableJSSwitch?.CheckedChange += EnableJSChecked;

            openLinkInBrowserSwitch = FindViewById< Switch >( Resource.Id.SettingsOpenLinkInBrowser );
            openLinkInBrowserSwitch?.Checked = settings.openLinkInBrowser;
            openLinkInBrowserSwitch?.CheckedChange += OpenLinkInBrowserChecked;
        }

        public override bool OnOptionsItemSelected( IMenuItem item )
        {
            switch( item.ItemId )
            {
                case Android.Resource.Id.Home:
                    ReturnToPrevActivity();
                    return true;
                default:
                    return base.OnOptionsItemSelected( item );
            }
        }

        public void MaxItemsPerFeedClicked( object? sender, EventArgs e )
        {
            if( maxItemsPerFeed != null )
            {
                AndroidX.AppCompat.Widget.PopupMenu popupMenu = new( this, maxItemsPerFeed );

                List< int > feedCounts = [ 50, 100, 150, 200, 250 ];
                for( int i = 0; i < feedCounts.Count; ++i )
                    popupMenu.Menu.Add( feedCounts[ i ].ToString() );
                
                popupMenu.MenuItemClick += ( menu, args ) =>
                {
                    if( args?.Item != null )
                    {
                        for( int i = 0; i < feedCounts.Count; ++i )
                        {
                            if( feedCounts[ i ].ToString() == args.Item.TitleFormatted?.ToString() )
                            {
                                settings.maxItemsPerFeed = feedCounts[ i ];
                                maxItemsPerFeedCount?.Text = settings.maxItemsPerFeed.ToString();
                                break;
                            }
                        }
                    }
                };
                popupMenu.Show();
            }
        }

        public void OpenLinkAutomaticallyChecked( object? sender, CompoundButton.CheckedChangeEventArgs e )
        {
            if( openLinkAutomaticallySwitch != null )
            {
                openLinkAutomaticallySwitch.Checked = e.IsChecked;
                settings.openLinkAutomatically = openLinkAutomaticallySwitch.Checked;
            }
        }

        public void EnableJSChecked( object? sender, CompoundButton.CheckedChangeEventArgs e )
        {
            if( enableJSSwitch != null )
            {
                enableJSSwitch.Checked = e.IsChecked;
                settings.enableJS = enableJSSwitch.Checked;
            }
        }

        public void OpenLinkInBrowserChecked( object? sender, CompoundButton.CheckedChangeEventArgs e )
        {
            if( openLinkInBrowserSwitch != null )
            {
                openLinkInBrowserSwitch.Checked = e.IsChecked;
                settings.openLinkInBrowser = openLinkInBrowserSwitch.Checked;
            }
        }


        private Settings settings = new();

        private LinearLayout? maxItemsPerFeed;
        private TextView? maxItemsPerFeedCount;
        private Switch? openLinkAutomaticallySwitch;
        private Switch? enableJSSwitch;
        private Switch? openLinkInBrowserSwitch;


        private void ReturnToPrevActivity()
        {
            Intent intent = new Intent( this, typeof( SettingsActivity ) );
            settings.SaveToIntent( intent, Resources );
            SetResult( Result.Ok, intent );
            Finish();
        }
    }

    class OnBackCallback : OnBackPressedCallback
    {
        public OnBackCallback( Action returnToPrevActivity, bool enabled ) : base( enabled )
        {
            this.returnToPrevActivity = returnToPrevActivity;
        }

        public override void HandleOnBackPressed()
        {
            returnToPrevActivity();
        }

        private Action returnToPrevActivity;
    }
}
