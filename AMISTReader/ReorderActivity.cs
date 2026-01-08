using Android.Content;
using Android.OS;
using Android.Views;
using AndroidX.AppCompat.App;
using AndroidX.RecyclerView.Widget;

namespace AMISTReader
{
    [ Activity( Label = "@string/Reorder" ) ]
    public class ReorderActivity : AppCompatActivity
    {
        protected override void OnCreate( Bundle? savedInstanceState )
        {
            base.OnCreate( savedInstanceState );
            SetContentView( Resource.Layout.ActivityReorder );

            OnBackCallback callback = new OnBackCallback( ReturnToPrevActivity, true );
            OnBackPressedDispatcher.AddCallback( this, callback );

            AndroidX.AppCompat.Widget.Toolbar? toolbar = FindViewById< AndroidX.AppCompat.Widget.Toolbar >( Resource.Id.Toolbar );
            SetSupportActionBar( toolbar );
            SupportActionBar?.SetDisplayHomeAsUpEnabled( true );
            SupportActionBar?.SetHomeButtonEnabled( true );

            layoutManager = new LinearLayoutManager( this );
            recyclerView = FindViewById< RecyclerView >( Resource.Id.ReorderRecyclerView );
            recyclerView?.SetLayoutManager( layoutManager );

            List< MyFeedsItem >? items;
            if( Build.VERSION.SdkInt >= BuildVersionCodes.Tiramisu )
            {
                #pragma warning disable CA1416 // Validate platform compatibility
                items = Intent?.GetParcelableArrayListExtra( Resources?.GetString( Resource.String.MyFeedsListKey ), Java.Lang.Class.FromType( typeof( MyFeedsItem ) ) )?.Cast< MyFeedsItem >().ToList();
                #pragma warning restore CA1416 // Validate platform compatibility
            }
            else
            {
                #pragma warning disable CA1422 // Validate platform compatibility
                items = Intent?.GetParcelableArrayListExtra( Resources?.GetString( Resource.String.MyFeedsListKey ) )?.Cast< MyFeedsItem >().ToList();
                #pragma warning restore CA1422 // Validate platform compatibility
            }

            if( items != null )
            {
                adapter = new( items, null, false );
                recyclerView?.SetAdapter( adapter );

                ReorderTouchHelperCallback touchHelperCallback = new( adapter );
                ItemTouchHelper touchHelper = new( touchHelperCallback );
                touchHelper.AttachToRecyclerView( recyclerView );
            }
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


        private RecyclerView? recyclerView;
        private RecyclerView.LayoutManager? layoutManager;
        private MyFeeds.Adapter? adapter;

        private void ReturnToPrevActivity()
        {
            Intent intent = new Intent( this, typeof( ReorderActivity ) );
            intent.PutParcelableArrayListExtra( Resources?.GetString( Resource.String.MyFeedsListKey ), adapter?.items.Cast< IParcelable >().ToList() );
            SetResult( Result.Ok, intent );
            Finish();
        }
    }

    public class ReorderTouchHelperCallback : ItemTouchHelper.Callback
    {
        public ReorderTouchHelperCallback( MyFeeds.Adapter adapter )
        {
            this.adapter = adapter;
        }

        public override int GetMovementFlags( RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder )
        {
            int dragFlags = ItemTouchHelper.Up | ItemTouchHelper.Down;
            return MakeMovementFlags( dragFlags, 0 );
        }

        public override bool OnMove( RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target )
        {
            adapter.OnItemMove( viewHolder.AbsoluteAdapterPosition, target.AbsoluteAdapterPosition );
            return true;
        }

        public override void OnSelectedChanged( RecyclerView.ViewHolder? viewHolder, int actionState )
        {
            if( actionState != ItemTouchHelper.ActionStateIdle )
            {
                if( viewHolder is MyFeeds.ItemHolder )
                {
                    MyFeeds.ItemHolder feedViewHolder = ( MyFeeds.ItemHolder ) viewHolder;
                    feedViewHolder.OnItemSelected();
                }
            }

            base.OnSelectedChanged( viewHolder, actionState );
        }

        public override void ClearView( RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder )
        {
            base.ClearView( recyclerView, viewHolder );

            if( viewHolder is MyFeeds.ItemHolder )
            {
                MyFeeds.ItemHolder itemHolder = ( MyFeeds.ItemHolder ) viewHolder;
                itemHolder.OnItemClear();
            }
        }

        public static new bool IsLongPressDragEnabled()
        {
            return true;
        }

        public override void OnSwiped( RecyclerView.ViewHolder viewHolder, int direction ) {}


        private MyFeeds.Adapter adapter;
    }
}
