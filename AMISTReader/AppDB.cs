using Android.Content;
using Android.Database;
using Android.Database.Sqlite;
using Android.OS;
using Android.Runtime;
using Android.Util;
using Java.Interop;

namespace AMISTReader
{
    public class AppDBHelper : SQLiteOpenHelper
    {
        public AppDBHelper( Context context ) : base( context, AppDBHelper.filename, null, 1  )
        {
            //Log.Debug( "AppDB filename: ", AppDBHelper.filename );
            db = WritableDatabase;
        }

        public override void OnCreate( SQLiteDatabase? db ) {}
        public override void OnUpgrade( SQLiteDatabase? db, int oldVersion, int newVersion ) {}

        public async Task< CreateTableResult > CreateMyFeedsTable()
        {
            List< string > columnNames = [ INTEGER( nameof( MyFeedsItem.uid ), true, true ),
                                            INTEGER( nameof( MyFeedsItem.index ) ),
                                            TEXT( nameof( MyFeedsItem.title ) ),
                                            TEXT( nameof( MyFeedsItem.url ) ),
                                            INTEGER( nameof( MyFeedsItem.unreadCount ) ) ];
            string createTableString = CREATE_TABLE( true, MyFeedsItem.TABLE_NAME, columnNames );
            
            CreateTableResult result;
            try
            {
                await Task.Run( () => db?.ExecSQL( createTableString ) );
                result = new( CreateTableResult.Success );
            }
            catch( Android.Database.SQLException e )
            {
                Log.Debug( "CreateMyFeedsTable failed", e.Message ?? "" );
                result = new();
            }

            return result;
        }

        public async Task< CreateTableResult > CreateFeedItemTable()
        {
            List< string > columnNames = [ INTEGER( nameof( FeedItem.uid ), true, true ),
                                            INTEGER( nameof( FeedItem.feedUid ) ),
                                            TEXT( nameof( FeedItem.title ) ),
                                            TEXT( nameof( FeedItem.subtitle ) ),
                                            TEXT( nameof( FeedItem.desc ) ),
                                            TEXT( nameof( FeedItem.url ) ),
                                            BLOB( nameof( FeedItem.urlHash ) ),
                                            BIGINT( nameof( FeedItem.dateTime ) ),
                                            INTEGER( nameof( FeedItem.hasBeenRead ) ) ];
            string createTableString = CREATE_TABLE( true, FeedItem.TABLE_NAME, columnNames );
            
            CreateTableResult result;
            try
            {
                await Task.Run( () => db?.ExecSQL( createTableString ) );
                result = new( CreateTableResult.Success );
            }
            catch( Android.Database.SQLException e )
            {
                Log.Debug( "CreateFeedItemTable failed", e.Message ?? "" );
                result = new();
            }

            return result;
        }

        public void CloseDB()
        {
            db?.Close();
        }


        public async Task< List< MyFeedsItem > > GetMyFeedsAsync()
        {
            ICursor? cursor = await Task.Run( () => db?.Query( MyFeedsItem.TABLE_NAME, null, null, null, null, null, null ) );

            List< MyFeedsItem > myFeedsItem = new();

            if( cursor != null )
            {
                if( cursor.MoveToFirst() )
                {
                    do
                    {
                        myFeedsItem.Add( new MyFeedsItem{ uid = cursor.GetInt( cursor.GetColumnIndex( nameof( MyFeedsItem.uid ) ) ),
                                                            index = cursor.GetInt( cursor.GetColumnIndex( nameof( MyFeedsItem.index ) ) ),
                                                            title = cursor.GetString( cursor.GetColumnIndex( nameof( MyFeedsItem.title ) ) ) ?? "",
                                                            url = cursor.GetString( cursor.GetColumnIndex( nameof( MyFeedsItem.url ) ) )  ?? "",
                                                            unreadCount = cursor.GetInt( cursor.GetColumnIndex( nameof( MyFeedsItem.unreadCount ) ) ) } );
                    }
                    while( cursor.MoveToNext() );
                }
                cursor.Close();
            }

            return myFeedsItem;
        }

        public async Task< int > AddMyFeedsItemAsync( MyFeedsItem item )
        {
            ContentValues values = new ContentValues();
            values.Put( SQL_NAME( nameof( MyFeedsItem.index ) ), item.index );
            values.Put( SQL_NAME( nameof( MyFeedsItem.title ) ), item.title );
            values.Put( SQL_NAME( nameof( MyFeedsItem.url ) ), item.url );
            values.Put( SQL_NAME( nameof( MyFeedsItem.unreadCount ) ), item.unreadCount );

            long rowId = await Task.Run( () => db?.Insert( MyFeedsItem.TABLE_NAME, null, values ) ) ?? -1;
            int result = 0;

            if( rowId >= 0 )
            {
                result = 1;
                item.uid = ( int ) rowId;
            }

            return result;
        }

        public async Task< int > UpdateMyFeedsItemAsync( MyFeedsItem item )
        {
            List< MyFeedsItem > itemList = new() { item };
            return await UpdateMyFeedsItemAsync( itemList );
        }

        public async Task< int > UpdateMyFeedsItemAsync( List< MyFeedsItem > itemList )
        {
            int result = await Task.Run( () => {
                int rowsUpdated = 0;
                foreach( MyFeedsItem item in itemList )
                {
                    ContentValues values = new ContentValues();
                    values.Put( SQL_NAME( nameof( MyFeedsItem.index ) ), item.index );
                    values.Put( SQL_NAME( nameof( MyFeedsItem.title ) ), item.title );
                    values.Put( SQL_NAME( nameof( MyFeedsItem.url ) ), item.url );
                    values.Put( SQL_NAME( nameof( MyFeedsItem.unreadCount ) ), item.unreadCount );
                    rowsUpdated += db?.Update( MyFeedsItem.TABLE_NAME, values, nameof( item.uid ) + " = ?", [ $"{ item.uid }" ] ) ?? 0;
                }
                return rowsUpdated;
            } );

            return result;
        }

        public async Task< int > DeleteMyFeedsItemAsync( MyFeedsItem item )
        {
            int result = await Task.Run( () => db?.Delete( MyFeedsItem.TABLE_NAME, nameof( item.uid ) + " = ?", [ $"{ item.uid }" ] ) ) ?? 0;
            return result;
        }


        public async Task< List< FeedItem > > GetFeedItemAsync( int feedUid = -1 )
        {
            string? where = null;
            string[]? whereArgs = null;
            if( feedUid >= 0 )
            {
                where = nameof( FeedItem.feedUid ) + " = ?";
                whereArgs = [ $"{ feedUid }" ];
            }

            ICursor? cursor = await Task.Run( () => db?.Query( FeedItem.TABLE_NAME, null, where, whereArgs, null, null, null ) );

            List< FeedItem > feedItems = new();
            if( cursor != null )
            {
                if( cursor.MoveToFirst() )
                {
                    do
                    {
                        feedItems.Add( new FeedItem{ uid = cursor.GetInt( cursor.GetColumnIndex( nameof( FeedItem.uid ) ) ),
                                                        feedUid = cursor.GetInt( cursor.GetColumnIndex( nameof( FeedItem.feedUid) ) ),
                                                        title = cursor.GetString( cursor.GetColumnIndex( nameof( FeedItem.title ) ) ) ?? "",
                                                        subtitle = cursor.GetString( cursor.GetColumnIndex( nameof( FeedItem.subtitle ) ) ) ?? "",
                                                        desc = cursor.GetString( cursor.GetColumnIndex( nameof( FeedItem.desc ) ) ) ?? "",
                                                        url = cursor.GetString( cursor.GetColumnIndex( nameof( FeedItem.url ) ) )  ?? "",
                                                        urlHash = cursor.GetBlob( cursor.GetColumnIndex( nameof( FeedItem.urlHash ) ) ),
                                                        dateTime = new( cursor.GetLong( cursor.GetColumnIndex( nameof( FeedItem.dateTime ) ) ) ),
                                                        hasBeenRead = cursor.GetInt( cursor.GetColumnIndex( nameof( FeedItem.hasBeenRead ) ) ) == 1 } );
                    }
                    while( cursor.MoveToNext() );
                }
                cursor.Close();
            }

            return feedItems;
        }

        public async Task< int > AddFeedItemAsync( FeedItem item )
        {
            ContentValues values = new ContentValues();
            values.Put( SQL_NAME( nameof( FeedItem.feedUid ) ), item.feedUid );
            values.Put( SQL_NAME( nameof( FeedItem.title ) ), item.title );
            values.Put( SQL_NAME( nameof( FeedItem.subtitle ) ), item.subtitle );
            values.Put( SQL_NAME( nameof( FeedItem.desc ) ), item.desc );
            values.Put( SQL_NAME( nameof( FeedItem.url ) ), item.url );
            values.Put( SQL_NAME( nameof( FeedItem.urlHash ) ), item.urlHash );
            values.Put( SQL_NAME( nameof( FeedItem.dateTime ) ), item.dateTime != null ? item.dateTime.Value.Ticks : 0 );
            values.Put( SQL_NAME( nameof( FeedItem.hasBeenRead ) ), item.hasBeenRead );

            long rowId = await Task.Run( () => db?.Insert( FeedItem.TABLE_NAME, null, values ) ) ?? -1;
            int result = 0;

            if( rowId >= 0 )
            {
                result = 1;
                item.uid = ( int ) rowId;
            }

            return result;
        }

        public async Task< int > UpdateFeedItemAsync( FeedItem item )
        {
            List< FeedItem > itemList = new() { item };
            return await UpdateFeedItemAsync( itemList );
        }

        public async Task< int > UpdateFeedItemAsync( List< FeedItem > itemList )
        {
            int result = await Task.Run( () => {
                int rowsUpdated = 0;
                foreach( FeedItem item in itemList )
                {
                    ContentValues values = new ContentValues();
                    values.Put( SQL_NAME( nameof( FeedItem.feedUid ) ), item.feedUid );
                    values.Put( SQL_NAME( nameof( FeedItem.title ) ), item.title );
                    values.Put( SQL_NAME( nameof( FeedItem.subtitle ) ), item.subtitle );
                    values.Put( SQL_NAME( nameof( FeedItem.desc ) ), item.desc );
                    values.Put( SQL_NAME( nameof( FeedItem.url ) ), item.url );
                    values.Put( SQL_NAME( nameof( FeedItem.urlHash ) ), item.urlHash );
                    values.Put( SQL_NAME( nameof( FeedItem.dateTime ) ), item.dateTime != null ? item.dateTime.Value.Ticks : 0 );
                    values.Put( SQL_NAME( nameof( FeedItem.hasBeenRead ) ), item.hasBeenRead );
                    rowsUpdated += db?.Update( FeedItem.TABLE_NAME, values, nameof( item.uid ) + " = ?", [ $"{ item.uid }" ] ) ?? 0;
                }
                return rowsUpdated;
            } );

            return result;
        }

        public async Task< int > MarkAllAsRead( int feedUid = -1 )
        {
            string? where = null;
            string[]? whereArgs = null;
            if( feedUid >= 0 )
            {
                where = nameof( FeedItem.feedUid ) + " = ?";
                whereArgs = [ $"{ feedUid }" ];
            }

            ContentValues values = new ContentValues();
            values.Put( SQL_NAME( nameof( FeedItem.hasBeenRead ) ), true );

            int result = await Task.Run( () => db?.Update( FeedItem.TABLE_NAME, values, where, whereArgs ) ) ?? 0;
            return result;
        }

        public async Task< int > DeleteFeedItemAsync( FeedItem item )
        {
            int result = await Task.Run( () => db?.Delete( FeedItem.TABLE_NAME, nameof( FeedItem.uid ) + " = ?", [ $"{ item.uid }" ] ) ) ?? 0;
            return result;
        }

        public async Task< int > DeleteAllFeedItemsAsync()
        {
            int result = await Task.Run( () => db?.Delete( FeedItem.TABLE_NAME, null, null ) ) ?? 0;
            return result;
        }

        public async Task< int > DeleteAllFeedItemsAsync( MyFeedsItem item )
        {
            int result = await Task.Run( () => db?.Delete( FeedItem.TABLE_NAME, nameof( FeedItem.feedUid ) + " = ?", [ $"{ item.uid }" ] ) ) ?? 0;
            return result;
        }


        private SQLiteDatabase? db;
        static private string filename = Path.Combine( System.Environment.GetFolderPath( System.Environment.SpecialFolder.ApplicationData ), "App.db" );        //emulator - /data/user/0/amist.amistreader/files/.config/App.db

        static private string SQL_NAME( string name )
        {
            string sqlName = "\"" + name + "\"";
            return sqlName;
        }

        static private string CREATE_TABLE( bool ifNotExists, string tableName, List< string > columnNames )
        {
            string result = "CREATE TABLE ";

            if( ifNotExists )
                result += "IF NOT EXISTS ";

            result += SQL_NAME( tableName ) + " (";

            for( int i = 0; i < columnNames.Count; ++i )
            {
                result += columnNames[ i ];

                if( i + 1 < columnNames.Count )
                    result += ", ";
            }

            result += ");";
            return result;
        }

        static private string INTEGER( string columnName, bool isPrimaryKey = false, bool autoIncrement = false )
        {
            string result = SQL_NAME( columnName ) + " INTEGER";

            if( isPrimaryKey )
                result += " PRIMARY KEY";

            if( autoIncrement )
                result += " AUTOINCREMENT";

            return result;
        }

        static private string BIGINT( string columnName )
        {
            string result = SQL_NAME( columnName ) + " BIGINT";
            return result;
        }

        static private string TEXT( string columnName )
        {
            string result = SQL_NAME( columnName ) + " TEXT";
            return result;
        }

        static private string BLOB( string columnName )
        {
            string result = SQL_NAME( columnName ) + " BLOB";
            return result;
        }
    }

    public class MyFeedsItem : Java.Lang.Object, Android.OS.IParcelable
    {
        public const string TABLE_NAME = "MyFeedsItem";

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
        public const string TABLE_NAME = "FeedItem";

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


    public class CreateTableResult( int value = CreateTableResult.Failed )
    {
        public const int Failed = 0;
        public const int Success = 1;

        public int result = value;
    }
}
