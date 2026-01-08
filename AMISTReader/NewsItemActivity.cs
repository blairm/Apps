using Android.Content;
using Android.Text;
using Android.Text.Method;
using Android.Text.Style;
using Android.Views;
using Android.Webkit;
using Android.Widget;
using AndroidX.Activity;
using AndroidX.AppCompat.App;
using AndroidX.DrawerLayout.Widget;
using Java.Lang;

namespace AMISTReader
{
    [ Activity( Label = "@string/NewsItem" ) ]
    public class NewsItemActivity : AppCompatActivity
    {
        protected override void OnCreate( Bundle? savedInstanceState )
        {
            base.OnCreate( savedInstanceState );
            SetContentView( Resource.Layout.ActivityNewsItem );

            OnBackCallback callback = new OnBackCallback( ReturnToPrevActivity, true );
            OnBackPressedDispatcher.AddCallback( this, callback );

            index = Intent != null ? Intent.GetIntExtra( Resources?.GetString( Resource.String.NewsItemIndexKey ), index ) : index;

            AndroidX.AppCompat.Widget.Toolbar? toolbar = FindViewById< AndroidX.AppCompat.Widget.Toolbar >( Resource.Id.Toolbar );
            SetSupportActionBar( toolbar );
            SupportActionBar?.SetDisplayHomeAsUpEnabled( true );
            SupportActionBar?.SetHomeButtonEnabled( true );

            toolbarTitle = FindViewById< TextView >( Resource.Id.ToolbarTitle );
            toolbarTitle?.Text = Intent?.GetStringExtra( Resources?.GetString( Resource.String.NewsItemTitleKey) );

            content = FindViewById< TextView >( Resource.Id.Content );
            content?.TextFormatted = Html.FromHtml( Intent?.GetStringExtra( Resources?.GetString( Resource.String.NewsItemDescKey ) ), FromHtmlOptions.ModeLegacy );
            content?.MovementMethod = new LinkMovementMethod();

            bool openLinkInBrowser = Intent != null ? Intent.GetBooleanExtra( Resources?.GetString( Resource.String.SettingsOpenLinkInBrowser ), false ) : false;
            if( !openLinkInBrowser )
            {
                SpannableString? spannableString = ( SpannableString? ) content?.TextFormatted;
                Java.Lang.Object[]? urls = spannableString?.GetSpans( 0, spannableString.Length(), Class.FromType( typeof( URLSpan ) ) );

                if( spannableString != null )
                {
                    for( int i = 0; i < urls?.Length; ++i )
                    {
                        URLSpan? span   = ( URLSpan ) urls[ i ];
                        int start       = spannableString.GetSpanStart( span );
                        int end         = spannableString.GetSpanEnd( span );
                        SpanTypes flags = spannableString.GetSpanFlags( span );

                        WebViewURLSpan clickableSpan = new( span.URL, OpenWebViewActivity );
                        spannableString.SetSpan( clickableSpan, start, end, flags );
                        spannableString.RemoveSpan( span );
                    }
                }

                content?.TextFormatted = spannableString;
            }


            enableJS = Intent != null ? Intent.GetBooleanExtra( Resources?.GetString( Resource.String.SettingsEnableJSKey ), false ) : false;
        }

        public override bool OnCreateOptionsMenu( IMenu? menu )
        {
            MenuInflater.Inflate( Resource.Menu.NewsItemActivityOverflowMenu, menu );
            return true;
        }

        public override bool OnOptionsItemSelected( IMenuItem item )
        {
            switch( item.ItemId )
            {
                case Android.Resource.Id.Home:
                    ReturnToPrevActivity();
                    return true;
                case Resource.Id.OverflowMarkAsUnread:
                    hasRead = false;
                    ReturnToPrevActivity();
                    return true;
                default:
                    return base.OnOptionsItemSelected( item );
            }
        }


        private TextView? toolbarTitle;
        private TextView? content;

        private int index = -1;
        private bool hasRead = true;
        private bool enableJS = false;


        private void ReturnToPrevActivity()
        {
            Intent intent = new Intent( this, typeof( NewsItemActivity ) );
            intent.PutExtra( Resources?.GetString( Resource.String.NewsItemIndexKey ), index );
            intent.PutExtra( Resources?.GetString( Resource.String.NewsItemHasReadKey ), hasRead );
            SetResult( Result.Ok, intent );
            Finish();
        }

        private void OpenWebViewActivity( string url )
        {
            Intent intent = new( this, typeof( NewsItemWebViewActivity ) );
            intent.PutExtra( Resources?.GetString( Resource.String.SettingsEnableJSKey ), enableJS );
            intent.PutExtra( Resources?.GetString( Resource.String.NewsItemLinkKey ), url );
            StartActivity( intent );
        }
    }

    class WebViewURLSpan : URLSpan
    {
        public WebViewURLSpan( string? url, Action< string > openWebView ) : base( url )
        {
            this.openWebView = openWebView;
        }

        public override void OnClick( View? widget )
        {
            if( URL != null )
                openWebView( URL );
        }

        private Action< string > openWebView;
    }
}
