using Android.Content;
using Android.Content.Res;
using Android.Net;
using Android.OS;
using Android.Util;
using Android.Views;
using AndroidX.Activity.Result;
using AndroidX.Activity.Result.Contract;
using AndroidX.AppCompat.App;
using AndroidX.Core.View;
using AndroidX.DrawerLayout.Widget;
using AndroidX.RecyclerView.Widget;
using AndroidX.SwipeRefreshLayout.Widget;
using CodeHollow.FeedReader;
using System.Security.Cryptography;
using System.Text;

namespace AMISTReader
{
    [ Activity( Label = "@string/app_name", MainLauncher = true ) ]
    public class MainActivity : AppCompatActivity, View.IOnClickListener, SwipeRefreshLayout.IOnRefreshListener
    {
        protected override void OnCreate( Bundle? savedInstanceState )
        {
            base.OnCreate( savedInstanceState );
            SetContentView( Resource.Layout.ActivityMain );

            AndroidX.AppCompat.Widget.Toolbar? toolbar = FindViewById< AndroidX.AppCompat.Widget.Toolbar >( Resource.Id.Toolbar );
            SetSupportActionBar( toolbar );


            drawerLayout = FindViewById< DrawerLayout >( Resource.Id.DrawerLayout );
            drawerLayout?.RequestDisallowInterceptTouchEvent( true );

            ActionBarDrawerToggle drawerToggle = new( this,
                                                        drawerLayout,
                                                        toolbar,
                                                        0,
                                                        0);

            if( drawerLayout != null )
                drawerLayout.AddDrawerListener( drawerToggle );

            drawerToggle.SyncState();


            toolbarTitle = drawerLayout?.FindViewById< TextView >( Resource.Id.AllFeedsTitle );

            
            LinearLayout? sideMenu = FindViewById< LinearLayout >( Resource.Id.SideMenu );

            sideMenuOverflow = sideMenu?.FindViewById< ImageView >( Resource.Id.SideMenuOverflow );
            sideMenuOverflow?.SetOnClickListener( this );

            allFeedsItem = sideMenu?.FindViewById< TextView >( Resource.Id.AllFeedsTitle );
            allFeedsItem?.SetOnClickListener( this );

            myFeedsLayoutManager = new LinearLayoutManager( this );
            myFeedsRecyclerView = sideMenu?.FindViewById< RecyclerView >( Resource.Id.SideMenuRecyclerView );
            myFeedsRecyclerView?.SetLayoutManager( myFeedsLayoutManager );

            myFeedsAdapter = new( [], OnFeedSelected );
            myFeedsRecyclerView?.SetAdapter( myFeedsAdapter );

            appDBHelper = new();
            appDBHelper.CreateMyFeedsTable().ContinueWith( results =>
            {
                if( results.Result == SQLite.CreateTableResult.Migrated )
                {
                    appDBHelper.GetMyFeedsAsync().ContinueWith( results =>
                    {
                        myFeedsAdapter.items = results.Result.OrderBy( myFeedsItem => myFeedsItem.index ).ToList< MyFeedsItem >();

                        RunOnUiThread( () =>
                        {
                            myFeedsAdapter.NotifyDataSetChanged();
                        } );
                    } );
                }
            } );


            newsSwipeRefreshLayout = FindViewById< SwipeRefreshLayout >( Resource.Id.NewsSwipeRefreshLayout );
            newsRecyclerView = FindViewById< RecyclerView >( Resource.Id.NewsRecyclerView );

            newsLayoutManager = new LinearLayoutManager( this );
            newsRecyclerView?.SetLayoutManager( newsLayoutManager );

            newsSwipeRefreshLayout?.SetColorSchemeResources( Android.Resource.Color.DarkerGray );
            newsSwipeRefreshLayout?.SetOnRefreshListener( this );

            newsAdapter = new( this, [], OnNewsItemSelected );
            newsRecyclerView?.SetAdapter( newsAdapter );

            appDBHelper.CreateFeedItemTable().ContinueWith( results =>
            {
                if( results.Result == SQLite.CreateTableResult.Migrated )
                {
                    //@NOTE uncomment to clear FeedItems table
                    //appDBHelper.DeleteAllFeedItemsAsync().Wait();
                    appDBHelper.GetFeedItemAsync().ContinueWith( results =>
                    {
                        newsAdapter.items = results.Result.OrderByDescending( feedItem => feedItem.dateTime ).ToList< FeedItem >();

                        RunOnUiThread( () =>
                        {
                            newsAdapter.NotifyDataSetChanged();
                        } );
                    } );
                }
            } );


            reorderResultLauncher   = RegisterForActivityResult( new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback( OnReorderActivityResult ) );
            settingsResultLauncher  = RegisterForActivityResult( new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback( OnSettingsActivityResult ) );
            newsItemResultLauncher  = RegisterForActivityResult( new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback( OnNewsItemActivityResult ) );


            settings.Load( GetPreferences( FileCreationMode.Private ), Resources );
        }

        protected override void OnDestroy()
        {
            appDBHelper?.CloseDB();
            base.OnDestroy();
        }

        public override bool OnCreateOptionsMenu( IMenu? menu )
        {
            MenuInflater.Inflate( Resource.Menu.MainActivityOverflowMenu, menu );
            return true;
        }
        

        public async void OnRefresh()
        {
            if( myFeedsAdapter != null )
            {
                if( myFeedsAdapter.items.Count == 0 )
                {
                    newsSwipeRefreshLayout?.Refreshing = false;
                    return;
                }
                else if( !HasNetworkConnection() )
                {
                    Toast.MakeText( this, Resource.String.NotConnectedMessage, ToastLength.Short )?.Show();

                    newsSwipeRefreshLayout?.Refreshing = false;
                    return;
                }

                int startIndex = 0;
                int endIndex = myFeedsAdapter.items.Count;

                if( currFeed >= 0 )
                {
                    startIndex = currFeed;
                    endIndex = startIndex + 1;
                }

                for( int i = startIndex; i < endIndex; ++i )
                {
                    MyFeedsItem myFeedsItem = myFeedsAdapter.items[ i ];
                    await FeedReader.ReadAsync( myFeedsItem.url ).ContinueWith( results =>
                    {
                        if( results.Status == TaskStatus.RanToCompletion )
                        {
                            if( newsAdapter != null )
                            {
                                Feed feed = results.Result;
                                if( newsAdapter.items.Capacity < newsAdapter.items.Count + feed.Items.Count )
                                    newsAdapter.items.Capacity = newsAdapter.items.Count + feed.Items.Count;

                                byte[] urlHash = [];
                                for( int a = 0; a < feed.Items.Count; ++a )
                                {
                                    CodeHollow.FeedReader.FeedItem frFeedItem = feed.Items[ a ];
                                    urlHash = MD5.HashData( Encoding.UTF8.GetBytes( frFeedItem.Link ) );
                                    Span< byte > urlHashSpan = urlHash.AsSpan();

                                    bool found = false;
                                    for( int b = 0; b < newsAdapter.items.Count; ++b )
                                    {
                                        FeedItem feedItem = newsAdapter.items[ b ];
                                        if( feedItem.feedUid == myFeedsItem.uid && urlHashSpan.SequenceEqual( feedItem.urlHash.AsSpan() ) )
                                        {
                                            found = true;
                                            break;
                                        }
                                    }

                                    if( !found )
                                    {
                                        FeedItem item = new() { uid = 0,
                                                                feedUid = myFeedsItem.uid,
                                                                title = frFeedItem.Title,
                                                                subtitle = feed.Title,
                                                                desc = ( frFeedItem.Content != null && frFeedItem.Content != "" ) ? frFeedItem.Content : frFeedItem.Description,
                                                                url = frFeedItem.Link,
                                                                urlHash = urlHash,
                                                                dateTime = frFeedItem.PublishingDate,
                                                                hasBeenRead = false };

                                        appDBHelper?.AddFeedItemAsync( item ).Wait();
                                        newsAdapter.items.Add( item );
                                    }
                                }

                                newsAdapter.items.Sort( ( a, b ) =>
                                {
                                    if( a.dateTime == null && b.dateTime == null )
                                        return 0;
                                    else if( a.dateTime == null )
                                        return -1;
                                    else if( b.dateTime == null )
                                        return 1;

                                    return b.dateTime.Value.CompareTo( a.dateTime.Value );
                                } );

                                myFeedsItem.unreadCount = 0;
                                int feedItemCount = 0;
                                for( int a = 0; a < newsAdapter.items.Count; ++a )
                                {
                                    FeedItem feedItem = newsAdapter.items[ a ];
                                    if( feedItem.feedUid == myFeedsItem.uid )
                                    {
                                        if( ++feedItemCount > settings.maxItemsPerFeed )
                                        {
                                            appDBHelper?.DeleteFeedItemAsync( feedItem ).Wait();
                                            newsAdapter.items.RemoveAt( a );
                                            --a;
                                        }
                                        else if( !feedItem.hasBeenRead )
                                        {
                                            ++myFeedsItem.unreadCount;
                                        }
                                    }
                                }

                                appDBHelper?.UpdateMyFeedsItemAsync( myFeedsItem ).Wait();

                                RunOnUiThread( () =>
                                {
                                    newsAdapter?.NotifyDataSetChanged();
                                    newsSwipeRefreshLayout?.Refreshing = false;

                                    myFeedsAdapter?.NotifyItemChanged( i );
                                } );
                            }
                        }
                        else
                        {
                            RunOnUiThread( () =>
                            {
                                string? feedTitle = myFeedsAdapter?.items[ i ].title;
                                if( feedTitle != null && feedTitle != "" && Resources != null )
                                    Toast.MakeText( this, feedTitle + Resources.GetString( Resource.String.FeedFailedToLoad ), ToastLength.Short )?.Show();
                                else
                                    Toast.MakeText( this, Resource.String.UnknownFeedFailedToLoad, ToastLength.Short )?.Show();

                                newsSwipeRefreshLayout?.Refreshing = false;
                            } );
                        }
                    } );
                }
            }
        }

        public override bool OnOptionsItemSelected( IMenuItem item )
        {
            switch( item.ItemId )
            {
                case Resource.Id.OverflowMarkAllAsRead:
                    if( newsAdapter != null )
                    {
                        for( int i = 0; i < newsAdapter.items.Count; ++i )
                            newsAdapter.items[ i ].hasBeenRead = true;

                        appDBHelper?.UpdateFeedItemAsync( newsAdapter.items ).ContinueWith( results =>
                        {
                            RunOnUiThread( () =>
                            {
                                newsAdapter.NotifyDataSetChanged();
                            } );

                            if( myFeedsAdapter != null )
                            {
                                int startIndex = 0;
                                int endIndex = myFeedsAdapter.items.Count;

                                if( currFeed >= 0 )
                                {
                                    startIndex = currFeed;
                                    endIndex = startIndex + 1;
                                }

                                for( int i = startIndex; i < endIndex; ++i )
                                    myFeedsAdapter.items[ i ].unreadCount = 0;

                                appDBHelper?.UpdateMyFeedsItemAsync( myFeedsAdapter.items ).ContinueWith( results =>
                                {
                                    RunOnUiThread( () =>
                                    {
                                        myFeedsAdapter.NotifyDataSetChanged();
                                    } );
                                } );
                            }
                        } );
                    }
                    return true;
                case Resource.Id.OverflowSettings:
                    Intent intent = new( this, typeof( SettingsActivity ) );
                    settings.SaveToIntent( intent, Resources );
                    settingsResultLauncher?.Launch( intent );
                    return true;
            }

            return base.OnOptionsItemSelected( item );
        }

        public override bool OnContextItemSelected( IMenuItem item )
        {
            int index = RecyclerView.NoPosition;
            if( myFeedsAdapter != null )
                index = myFeedsAdapter.ResetSelectedIndex();

            if( index > RecyclerView.NoPosition )
            {
                MyFeedsItem? myFeedsItem = myFeedsAdapter?.items[ index ];
                if( myFeedsItem != null )
                {
                    switch ( item.ItemId )
                    {
                        case 1:
                        {
                            LayoutInflater? inflater = LayoutInflater.From( this );
                            View? dialogView = inflater?.Inflate( Resource.Layout.DialogAddFeed, null );
                            if( dialogView != null )
                            {
                                EditText? feedTitleEdit = dialogView.FindViewById< EditText >( Resource.Id.FeedTitleEdit );
                                feedTitleEdit?.Text = myFeedsItem.title;
                                EditText? feedURLEdit = dialogView.FindViewById< EditText >( Resource.Id.FeedURLEdit );
                                feedURLEdit?.Text = myFeedsItem.url;

                                AndroidX.AppCompat.App.AlertDialog.Builder builder = new( this );
                                builder.SetView( dialogView );
                                builder.SetTitle( Resource.String.DialogAddFeedEdit );

                                builder.SetPositiveButton( Resource.String.DialogAddFeedOK, ( sender, args ) =>
                                {
                                    string? title   = feedTitleEdit?.Text;
                                    string? url     = AddHTTPSToURL( feedURLEdit?.Text );
                                    
                                    if( url != null )
                                    {
                                        //@NOTE update ui after db is updated
                                        MyFeedsItem newMyFeedsItem = new() { uid = myFeedsItem.uid, index = myFeedsItem.index, title = title, url = url, unreadCount = myFeedsItem.unreadCount };
                                        appDBHelper?.UpdateMyFeedsItemAsync( newMyFeedsItem ).ContinueWith( results =>
                                        {
                                            RunOnUiThread( () =>
                                            {
                                                myFeedsAdapter?.items[ index ] = newMyFeedsItem;
                                                myFeedsAdapter?.NotifyItemChanged( index );
                                            } );
                                        } );
                                    }
                                } );

                                builder.SetNegativeButton( Resource.String.DialogAddFeedCancel, ( sender, args ) =>
                                {
                                    Log.Debug( "DialogEditFeed", "Cancelled" );
                                } );

                                builder.Show();
                            }
                            return true;
                        }
                        case 2:
                            if( myFeedsAdapter != null )
                            {
                                int currFeedIndex = currFeed;
                                MyFeedsItem itemToDelete = myFeedsAdapter.items[ index ];
                                appDBHelper?.DeleteMyFeedsItemAsync( itemToDelete ).ContinueWith( results =>
                                {
                                    myFeedsAdapter.items.RemoveAt( index );
                                    RunOnUiThread( () =>
                                    {
                                        myFeedsAdapter.NotifyItemRemoved( index );
                                    } );

                                    int itemsToUpdateCount = myFeedsAdapter.items.Count - index;
                                    if( itemsToUpdateCount > 0 )
                                    {
                                        List< MyFeedsItem > itemsToUpdate = myFeedsAdapter.items.GetRange( index, itemsToUpdateCount );
                                        
                                        for( int i = 0; i < itemsToUpdate.Count; ++i )
                                            --itemsToUpdate[ i ].index;

                                        appDBHelper?.UpdateMyFeedsItemAsync( itemsToUpdate ).Wait();
                                    }

                                    appDBHelper?.GetFeedItemAsync( itemToDelete.uid ).ContinueWith( results =>
                                    {
                                        List< FeedItem > items = results.Result;
                                        for( int i = 0; i < items.Count; ++i )
                                            appDBHelper?.DeleteFeedItemAsync( items[ i ] ).Wait();

                                        RunOnUiThread( () =>
                                        {
                                            for( int i = 0; i < newsAdapter?.items.Count; ++i )
                                            {
                                                FeedItem feedItem = newsAdapter.items[ i ];
                                                if( feedItem.feedUid == itemToDelete.uid )
                                                {
                                                    newsAdapter.items.RemoveAt( i );
                                                    newsAdapter.NotifyItemRemoved( i );
                                                    --i;
                                                }
                                            }

                                            if( currFeed == itemToDelete.index )
                                                OnFeedSelected( -1 );
                                        } );
                                    } ).Wait();
                                } );
                            }

                            return true;
                    }
                }
            }

            return base.OnContextItemSelected( item );
        }


        public void OnClick( View? v )
        {
            if( v != null )
            {
                if( v.Id == allFeedsItem?.Id )
                {
                    OnFeedSelected( -1 );
                }
                else if( v.Id == sideMenuOverflow?.Id )
                {
                    AndroidX.AppCompat.Widget.PopupMenu popupMenu = new( this, v );
                    popupMenu.Inflate( Resource.Menu.SideMenuOverflowMenu );
                    popupMenu.MenuItemClick += ( menu, args ) =>
                    {
                        switch( args?.Item?.ItemId )
                        {
                            case Resource.Id.SideMenuAddFeed:
                            {
                                LayoutInflater? inflater = LayoutInflater.From( this );
                                View? dialogView = inflater?.Inflate( Resource.Layout.DialogAddFeed, null );
                                if( dialogView != null )
                                {
                                    EditText? feedTitleEdit = dialogView.FindViewById< EditText >( Resource.Id.FeedTitleEdit );
                                    EditText? feedURLEdit = dialogView.FindViewById< EditText >( Resource.Id.FeedURLEdit );

                                    AndroidX.AppCompat.App.AlertDialog.Builder builder = new( this );
                                    builder.SetView( dialogView );
                                    builder.SetTitle( Resource.String.DialogAddFeed );

                                    builder.SetPositiveButton( Resource.String.DialogAddFeedOK, ( sender, args ) =>
                                    {
                                        int index       = myFeedsAdapter != null ? myFeedsAdapter.items.Count : 0;
                                        string? title   = feedTitleEdit?.Text;
                                        string? url     = AddHTTPSToURL( feedURLEdit?.Text );
                                    
                                        if( url != null )
                                        {
                                            MyFeedsItem item = new() { uid = 0, index = index, title = title, url = url, unreadCount = 0 };
                                            appDBHelper?.AddMyFeedsItemAsync( item ).ContinueWith( results =>
                                            {
                                                RunOnUiThread( () =>
                                                {
                                                    myFeedsAdapter?.items.Add( item );
                                                    myFeedsAdapter?.NotifyItemInserted( index );
                                                } );
                                            } );
                                        }
                                    } );

                                    builder.SetNegativeButton( Resource.String.DialogAddFeedCancel, ( sender, args ) =>
                                    {
                                        Log.Debug( "DialogAddFeed", "Cancelled" );
                                    } );

                                    builder.Show();
                                }
                                break;
                            }
                            case Resource.Id.SideMenuReorder:
                            {
                                drawerLayout?.CloseDrawer( GravityCompat.Start );

                                Intent intent = new( this, typeof( ReorderActivity ) );
                            
                                if( myFeedsAdapter != null )
                                    intent.PutParcelableArrayListExtra( Resources?.GetString( Resource.String.MyFeedsListKey ), myFeedsAdapter.items.Cast< IParcelable >().ToList() );
                            
                                reorderResultLauncher?.Launch( intent );
                                break;
                            }
                            case Resource.Id.SideMenuSettings:
                            {
                                drawerLayout?.CloseDrawer( GravityCompat.Start );

                                Intent intent = new( this, typeof( SettingsActivity ) );
                                settings.SaveToIntent( intent, Resources );
                                settingsResultLauncher?.Launch( intent );
                                break;
                            }
                        }
                    };
                    popupMenu.Show();
                }
            }
        }


        private TextView? toolbarTitle;
        private DrawerLayout? drawerLayout;

        private SwipeRefreshLayout? newsSwipeRefreshLayout;
        private RecyclerView? newsRecyclerView;
        private RecyclerView.LayoutManager? newsLayoutManager;
        private NewsView.Adapter? newsAdapter;

        private ImageView? sideMenuOverflow;
        private RecyclerView? myFeedsRecyclerView;
        private RecyclerView.LayoutManager? myFeedsLayoutManager;
        private MyFeeds.Adapter? myFeedsAdapter;
        private TextView? allFeedsItem;

        private AppDBHelper? appDBHelper;

        private ActivityResultLauncher? reorderResultLauncher;
        private ActivityResultLauncher? settingsResultLauncher;
        private ActivityResultLauncher? newsItemResultLauncher;

        private Settings settings = new();

        private int currFeed = -1;


        private void OnFeedSelected( int index )
        {
            currFeed = index;
            drawerLayout?.CloseDrawer( GravityCompat.Start );

            toolbarTitle?.Text = "";
            if( currFeed < 0 )
                toolbarTitle?.Text = Resources?.GetString( Resource.String.AllFeedsTitle );
            else if( currFeed < myFeedsAdapter?.items.Count )
                toolbarTitle?.Text = myFeedsAdapter?.items[ currFeed ].title;

            newsAdapter?.items.Clear();
            newsAdapter?.NotifyDataSetChanged();

            int feedUid = -1;
            if( myFeedsAdapter != null && currFeed >= 0 )
                feedUid = myFeedsAdapter.items[ currFeed ].uid;
            
            appDBHelper?.GetFeedItemAsync( feedUid ).ContinueWith( results =>
            {
                RunOnUiThread( () =>
                {
                    newsAdapter?.items = results.Result.OrderByDescending( feedItem => feedItem.dateTime ).ToList< FeedItem >();
                    newsAdapter?.NotifyDataSetChanged();
                } );
            } );
        }

        private void OnNewsItemSelected( int index )
        {
            if( newsAdapter != null )
            {
                MarkNewsItemAsRead( newsAdapter?.items[ index ], index, true );

                if( settings.openLinkAutomatically )
                {
                    if( settings.openLinkInBrowser )
                    {
                        Android.Net.Uri? uri = Android.Net.Uri.Parse( newsAdapter?.items[ index ].url );
                        Intent intent = new( Intent.ActionView, uri );
                        StartActivity( intent );
                    }
                    else
                    {
                        Intent intent = new( this, typeof( NewsItemWebViewActivity ) );
                        intent.PutExtra( Resources?.GetString( Resource.String.SettingsEnableJSKey ), settings.enableJS );
                        intent.PutExtra( Resources?.GetString( Resource.String.NewsItemLinkKey ), newsAdapter?.items[ index ].url );
                        StartActivity( intent );
                    }
                }
                else
                {
                    Intent intent = new( this, typeof( NewsItemActivity ) );
                    intent.PutExtra( Resources?.GetString( Resource.String.SettingsOpenLinkInBrowser ), settings.openLinkInBrowser );
                    intent.PutExtra( Resources?.GetString( Resource.String.NewsItemIndexKey ), index );

                    int? myFeedsItemIndex = myFeedsAdapter?.items.FindIndex( item => item.uid == newsAdapter?.items[ index ].feedUid );
                    if( myFeedsItemIndex != null )
                    {
                        MyFeedsItem? myFeedsItem = myFeedsAdapter?.items[ myFeedsItemIndex.Value ];
                        if( myFeedsItem != null )
                            intent.PutExtra( Resources?.GetString( Resource.String.NewsItemTitleKey ), myFeedsItem.title );
                    }
                
                    intent.PutExtra( Resources?.GetString( Resource.String.NewsItemDescKey ), newsAdapter?.items[ index ].desc );
                    intent.PutExtra( Resources?.GetString( Resource.String.SettingsEnableJSKey ), settings.enableJS );
                    newsItemResultLauncher?.Launch( intent );
                }
            }
        }

        private void OnReorderActivityResult( ActivityResult activityResult )
        {
            if( activityResult.ResultCode == ( int ) Result.Ok && activityResult.Data != null )
            {
                List< MyFeedsItem >? items;
                if( Build.VERSION.SdkInt >= BuildVersionCodes.Tiramisu )
                {
                    #pragma warning disable CA1416 // Validate platform compatibility
                    items = activityResult.Data.GetParcelableArrayListExtra( Resources?.GetString( Resource.String.MyFeedsListKey ), Java.Lang.Class.FromType( typeof( MyFeedsItem ) ) )?.Cast< MyFeedsItem >().ToList();
                    #pragma warning restore CA1416 // Validate platform compatibility
                }
                else
                {
                    #pragma warning disable CA1422 // Validate platform compatibility
                    items = activityResult.Data.GetParcelableArrayListExtra( Resources?.GetString( Resource.String.MyFeedsListKey ) )?.Cast< MyFeedsItem >().ToList();
                    #pragma warning restore CA1422 // Validate platform compatibility
                }

                if( items != null )
                {
                    for( int i = 0; i < items.Count; ++i )
                        items[ i ].index = i;

                    appDBHelper?.UpdateMyFeedsItemAsync( items ).ContinueWith( results =>
                    {
                        RunOnUiThread( () =>
                        {
                            myFeedsAdapter?.items = items;
                            myFeedsAdapter?.NotifyDataSetChanged();
                        } );
                    } );
                }
            }
        }

        private void OnSettingsActivityResult( ActivityResult activityResult )
        {
            if( activityResult.ResultCode == ( int ) Result.Ok && activityResult.Data != null )
            {
                settings.Load( activityResult.Data, Resources );
                settings.Save( GetPreferences( FileCreationMode.Private ), Resources );
            }
        }

        private void OnNewsItemActivityResult( ActivityResult activityResult )
        {
            if( activityResult.ResultCode == ( int ) Result.Ok && activityResult.Data != null )
            {
                bool hasRead = activityResult.Data.GetBooleanExtra( Resources?.GetString( Resource.String.NewsItemHasReadKey ), true );
                if( !hasRead )
                {
                    int index = activityResult.Data.GetIntExtra( Resources?.GetString( Resource.String.NewsItemIndexKey ), -1 );
                    if( index >= 0 )
                        MarkNewsItemAsRead( newsAdapter?.items[ index ], index, false );
                }
            }
        }

        private void MarkNewsItemAsRead( FeedItem? feedItem, int index, bool hasBeenRead )
        {
            if( feedItem != null && feedItem.hasBeenRead != hasBeenRead )
            {
                feedItem.hasBeenRead = hasBeenRead;
                appDBHelper?.UpdateFeedItemAsync( feedItem ).ContinueWith( results =>
                {
                    RunOnUiThread( () =>
                    {
                        newsAdapter?.NotifyItemChanged( index );
                    } );


                    int? myFeedsItemIndex = myFeedsAdapter?.items.FindIndex( item => item.uid == feedItem.feedUid );
                    if( myFeedsItemIndex != null )
                    {
                        MyFeedsItem? myFeedsItem = myFeedsAdapter?.items[ myFeedsItemIndex.Value ];
                        if( myFeedsItem != null )
                        {
                            if( hasBeenRead )
                                --myFeedsItem.unreadCount;
                            else
                                ++myFeedsItem.unreadCount;

                            appDBHelper?.UpdateMyFeedsItemAsync( myFeedsItem ).ContinueWith( results =>
                            {
                                RunOnUiThread( () =>
                                {
                                    myFeedsAdapter?.NotifyItemChanged( myFeedsItemIndex.Value );
                                } );
                            } );
                        }
                    }
                } );
            }
        }

        private bool HasNetworkConnection()
        {
            bool result = false;

            ConnectivityManager? connectivityManager = ( ConnectivityManager? ) GetSystemService( Context.ConnectivityService );
            if( connectivityManager != null )
            {
                NetworkCapabilities? networkCapabilities = connectivityManager.GetNetworkCapabilities( connectivityManager.ActiveNetwork );
                if( networkCapabilities != null )
                    result = networkCapabilities.HasTransport( TransportType.Wifi ) || networkCapabilities.HasTransport( TransportType.Cellular );
            }

            return result;
        }

        private string? AddHTTPSToURL( string? url )
        {
            string? result = url;
            if( url != null )
            {
                string http     = "http";
                string https    = "https";
                if( !url.StartsWith( http ) && !url.StartsWith( https ) )
                    result = new UriBuilder( https, url ).Uri.ToString();
            }

            return result;
        }
    }


    public class ActivityResultCallback : Java.Lang.Object, IActivityResultCallback
    {
        public ActivityResultCallback( Action< ActivityResult > resultCallback )
        {
            callback = resultCallback;
        }

        public void OnActivityResult( Java.Lang.Object? result )
        {
            ActivityResult? activityResult = ( ActivityResult? ) result;
            if( activityResult != null )
                callback( activityResult );
        }


        private Action< ActivityResult > callback;
    }


    public class Settings
    {
        public int maxItemsPerFeed = 100;
        public bool openLinkAutomatically = false;
        public bool enableJS = false;
        public bool openLinkInBrowser = false;

        public void Load( ISharedPreferences? sharedPref, Resources? resources )
        {
            if( sharedPref != null )
            {
                maxItemsPerFeed         = sharedPref.GetInt( resources?.GetString( Resource.String.SettingsMaxItemsPerFeedKey ), maxItemsPerFeed );
                openLinkAutomatically   = sharedPref.GetBoolean( resources?.GetString( Resource.String.SettingsOpenLinkAutomaticallyKey ), openLinkAutomatically );
                enableJS                = sharedPref.GetBoolean( resources?.GetString( Resource.String.SettingsEnableJSKey ), enableJS );
                openLinkInBrowser       = sharedPref.GetBoolean( resources?.GetString( Resource.String.SettingsOpenLinkInBrowserKey ), openLinkInBrowser );
            }
        }

        public void Load( Intent? intent, Resources? resources )
        {
            if( intent != null )
            {
                maxItemsPerFeed         = intent.GetIntExtra( resources?.GetString( Resource.String.SettingsMaxItemsPerFeedKey ), maxItemsPerFeed );
                openLinkAutomatically   = intent.GetBooleanExtra( resources?.GetString( Resource.String.SettingsOpenLinkAutomaticallyKey ), openLinkAutomatically );
                enableJS                = intent.GetBooleanExtra( resources?.GetString( Resource.String.SettingsEnableJSKey ), enableJS );
                openLinkInBrowser       = intent.GetBooleanExtra( resources?.GetString( Resource.String.SettingsOpenLinkInBrowserKey ), openLinkInBrowser);
            }
        }

        public void Save( ISharedPreferences? sharedPref, Resources? resources )
        {
            if( sharedPref != null )
            {
                ISharedPreferencesEditor? sharedEditor = sharedPref.Edit();
                if( sharedEditor != null )
                {
                    sharedEditor.PutInt( resources?.GetString( Resource.String.SettingsMaxItemsPerFeedKey ), maxItemsPerFeed );
                    sharedEditor.PutBoolean( resources?.GetString( Resource.String.SettingsOpenLinkAutomaticallyKey ), openLinkAutomatically );
                    sharedEditor.PutBoolean( resources?.GetString( Resource.String.SettingsEnableJSKey ), enableJS );
                    sharedEditor.PutBoolean( resources?.GetString( Resource.String.SettingsOpenLinkInBrowserKey ), openLinkInBrowser );
                    sharedEditor.Apply();
                }
            }
        }

        public void SaveToIntent( Intent intent, Resources? resources )
        {
            intent.PutExtra( resources?.GetString( Resource.String.SettingsMaxItemsPerFeedKey ), maxItemsPerFeed );
            intent.PutExtra( resources?.GetString( Resource.String.SettingsOpenLinkAutomaticallyKey ), openLinkAutomatically );
            intent.PutExtra( resources?.GetString( Resource.String.SettingsEnableJSKey ), enableJS );
            intent.PutExtra( resources?.GetString( Resource.String.SettingsOpenLinkInBrowserKey ), openLinkInBrowser );
        }
    }
}