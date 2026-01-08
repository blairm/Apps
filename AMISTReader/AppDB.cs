using Android.OS;
using Android.Runtime;
using Android.Util;
using Java.Interop;
using SQLite;

namespace AMISTReader
{
    public class AppDBHelper
    {
        public AppDBHelper()
        {
            string filename = Path.Combine( System.Environment.GetFolderPath( System.Environment.SpecialFolder.ApplicationData ), "App.db" );
            appDB = new SQLiteAsyncConnection( filename );
        }

        public async Task< SQLite.CreateTableResult > CreateMyFeedsTable()
        {
            SQLite.CreateTableResult result;
            try
            {
                result = await appDB.CreateTableAsync< MyFeedsItem >();
            }
            catch( Exception e )
            {
                Log.Debug( "CreateMyFeedsTable failed", e.Message );
                result = new();
            }

            return result;
        }

        public async Task< SQLite.CreateTableResult > CreateFeedItemTable()
        {
            SQLite.CreateTableResult result;
            try
            {
                result = await appDB.CreateTableAsync< FeedItem >();
            }
            catch( Exception e )
            {
                Log.Debug( "CreateFeedItemTable failed", e.Message );
                result = new();
            }

            return result;
        }

        public void CloseDB()
        {
            appDB.CloseAsync().Wait();
        }


        public async Task< List< MyFeedsItem > > GetMyFeedsAsync()
        {
            return await appDB.Table< MyFeedsItem >().ToListAsync();
        }

        public async Task< MyFeedsItem > GetMyFeedsItemAsync( int id )
        {
            return await appDB.Table< MyFeedsItem >().Where( i => i.index == id ).FirstOrDefaultAsync();
        }

        public async Task< int > AddMyFeedsItemAsync( MyFeedsItem item )
        {
            return await appDB.InsertAsync( item );
        }

        public async Task< int > UpdateMyFeedsItemAsync( MyFeedsItem item )
        {
            return await appDB.UpdateAsync( item );
        }

        public async Task< int > UpdateMyFeedsItemAsync( List< MyFeedsItem > itemList )
        {
            return await appDB.UpdateAllAsync( itemList );
        }

        public async Task< int > DeleteMyFeedsItemAsync( MyFeedsItem item )
        {
            return await appDB.DeleteAsync( item );
        }


        public async Task< List< FeedItem > > GetFeedItemAsync( int feedUid = -1 )
        {
            if( feedUid < 0 )
                return await appDB.Table< FeedItem >().ToListAsync();
            else
                return await appDB.Table< FeedItem >().Where( i => i.feedUid == feedUid ).ToListAsync();
        }

        public async Task< int > AddFeedItemAsync( FeedItem item )
        {
            return await appDB.InsertAsync( item );
        }

        public async Task< int > UpdateFeedItemAsync( FeedItem item )
        {
            return await appDB.UpdateAsync( item );
        }

        public async Task< int > UpdateFeedItemAsync( List< FeedItem > itemList )
        {
            return await appDB.UpdateAllAsync( itemList );
        }

        public async Task< int > DeleteFeedItemAsync( FeedItem item )
        {
            return await appDB.DeleteAsync( item );
        }

        public async Task< int > DeleteAllFeedItemsAsync()
        {
            return await appDB.DeleteAllAsync< FeedItem >();
        }


        private SQLiteAsyncConnection appDB;
    }

    public class MyFeedsItem : Java.Lang.Object, Android.OS.IParcelable
    {
        [PrimaryKey, AutoIncrement]
        public int uid { get; set; }
        public int index { get; set; }
        public string? title { get; set; }
        public string? url { get; set; }
        public int unreadCount { get; set; }

        public int DescribeContents()
        {
            return 0;
        }

        public void WriteToParcel( Parcel dest, [GeneratedEnum] ParcelableWriteFlags flags )
        {
            dest.WriteInt( uid );
            dest.WriteInt( index );
            dest.WriteString( title );
            dest.WriteString( url );
            dest.WriteInt( unreadCount );
        }

        [ExportField("CREATOR")]
        public static MyFeedsItemCreator InitializeCreator()
        {
            return new MyFeedsItemCreator();
        }

        public class MyFeedsItemCreator : Java.Lang.Object, IParcelableCreator
        {
            public Java.Lang.Object CreateFromParcel( Android.OS.Parcel? source )
            {
                MyFeedsItem item;
                if( source != null )
                {
                    item = new()
                    {
                        uid         = source.ReadInt(),
                        index       = source.ReadInt(),
                        title       = source.ReadString(),
                        url         = source.ReadString(),
                        unreadCount = source.ReadInt()
                    };
                }
                else
                {
                    item = new();
                }

                return item;
            }

            public Java.Lang.Object[] NewArray( int size ) => new MyFeedsItem[ size ];
        }
    }

    public class FeedItem
    {
        [PrimaryKey, AutoIncrement]
        public int uid { get; set; }            = -1;
        public int feedUid { get; set; }        = -1;
        public string? title { get; set; }      = "";
        public string? subtitle { get; set; }   = "";
        public string? desc { get; set; }       = "";
        public string? url { get; set; }        = "";
        public byte[]? urlHash { get; set; }    = [];
        public DateTime? dateTime { get; set; } = DateTime.MinValue;
        public bool hasBeenRead { get; set; }   = false;
    }
}
