using Android.Webkit;
using AndroidX.AppCompat.App;

namespace AMISTReader
{
    [ Activity( Label = "@string/NewsItem" ) ]
    public class NewsItemWebViewActivity : AppCompatActivity
    {
        protected override void OnCreate( Bundle? savedInstanceState )
        {
            base.OnCreate( savedInstanceState );
            SetContentView( Resource.Layout.ActivityNewsItemWebView );

            webView = FindViewById< WebView >( Resource.Id.WebView );
            webView?.Settings.JavaScriptEnabled = Intent != null ? Intent.GetBooleanExtra( Resources?.GetString( Resource.String.SettingsEnableJSKey ), false ) : false;

            string? url = Intent?.GetStringExtra( Resources?.GetString( Resource.String.NewsItemLinkKey ) );
            if( url != null )
                webView?.LoadUrl( url );
            else
                Finish();
        }


        private WebView? webView;
    }
}