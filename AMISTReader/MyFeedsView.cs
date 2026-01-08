using Android.Animation;
using Android.Graphics;
using Android.Graphics.Drawables;
using Android.Views;
using Android.Views.Animations;
using AndroidX.RecyclerView.Widget;
using System.Drawing;
using Color = Android.Graphics.Color;

namespace AMISTReader
{
    namespace MyFeeds
    {
        public class ItemHolder : RecyclerView.ViewHolder, View.IOnCreateContextMenuListener, View.IOnLongClickListener, View.IOnClickListener
        {
            public TextView? titleTV;
            public TextView? unreadCountTV;

            public ItemHolder( View itemView, Action< int >? itemClicked, bool addContextMenu ) : base( itemView )
            {
                titleTV         = itemView.FindViewById< TextView >( Resource.Id.MyFeedsTitle );
                unreadCountTV   = itemView.FindViewById< TextView >( Resource.Id.MyFeedsUnreadCount );

                this.itemClicked = itemClicked;
                if( this.itemClicked != null )
                    itemView.SetOnClickListener( this );

                if( addContextMenu )
                {
                    itemView.SetOnCreateContextMenuListener( this );
                    itemView.SetOnLongClickListener( this );
                }
            }

            public void OnCreateContextMenu( IContextMenu? menu, View? v, IContextMenuContextMenuInfo? menuInfo )
            {
                menu?.Add( IMenu.None, 1, IMenu.None, Resource.String.ContextMyFeedsEdit );
                menu?.Add( IMenu.None, 2, IMenu.None, Resource.String.ContextMyFeedsDelete );
            }

            public bool OnLongClick( View? v )
            {
                if( BindingAdapter != null )
                {
                    Adapter adapter = ( Adapter ) BindingAdapter;
                    adapter.selectedIndex = BindingAdapterPosition;

                    return false;
                }

                return true;
            }

            public void OnItemSelected()
            {
                ObjectAnimator? animator = ObjectAnimator.OfArgb( ItemView, "backgroundColor", Color.LightGray.ToArgb() );
                animator?.SetDuration( colourFadeTime );
                animator?.SetInterpolator( new LinearInterpolator() );
                animator?.Start();
            }

            public void OnItemClear()
            {
                int currColour = Color.LightGray.ToArgb();
                if( ItemView.Background is ColorDrawable colorDrawable )
                    currColour = colorDrawable.Color;

                ValueAnimator? animator = ValueAnimator.OfObject( new ArgbEvaluator(), currColour, Color.LightGray.ToArgb() & 0xFFFFFF );
                animator?.SetDuration( colourFadeTime );
                animator?.SetInterpolator( new LinearInterpolator() );
                animator?.Update += ( sender, args ) =>
                {
                    if( args.Animation.AnimatedValue != null )
                    {
                        int color = ( int ) args.Animation.AnimatedValue;
                        ItemView.SetBackgroundColor( new Color( color ) );
                    }
                };
                animator?.Start();
            }

            public void OnClick( View? v )
            {
                if( itemClicked != null )
                    itemClicked( BindingAdapterPosition );
            }

            private Action< int >? itemClicked;
            private int colourFadeTime = 100;
        }

        public class Adapter : RecyclerView.Adapter
        {
            public List< MyFeedsItem > items;
            public int selectedIndex;

            public Adapter( List< MyFeedsItem > newItems, Action< int >? itemClicked = null, bool addContextMenu = true )
            {
                items = newItems;
                ResetSelectedIndex();

                this.itemClicked = itemClicked;
                this.addContextMenu = addContextMenu;
            }

            public int ResetSelectedIndex()
            {
                int oldIndex = selectedIndex;
                selectedIndex = RecyclerView.NoPosition;
                return oldIndex;
            }

            public override RecyclerView.ViewHolder OnCreateViewHolder( ViewGroup parent, int viewType )
            {
                LayoutInflater? layoutInflater = LayoutInflater.From( parent.Context );
                View? itemView = layoutInflater?.Inflate( Resource.Layout.MyFeedsItem, parent, false );

                if( itemView == null )
                    itemView = new View( null );

                return new ItemHolder( itemView, this.itemClicked, addContextMenu );
            }

            public override void OnBindViewHolder( RecyclerView.ViewHolder holder, int position )
            {
                ItemHolder? itemHolder = holder as ItemHolder;
                itemHolder?.titleTV?.Text = items[ position ].title;

                int unreadCount = items[ position ].unreadCount;
                if( unreadCount > 0 )
                    itemHolder?.unreadCountTV?.Text = unreadCount.ToString();
                else
                    itemHolder?.unreadCountTV?.Text = "";
            }

            public void OnItemMove( int fromPosition, int toPosition )
            {
                MyFeedsItem swap = items[ fromPosition ];
                items.RemoveAt( fromPosition );
                items.Insert( toPosition, swap );
                NotifyItemMoved( fromPosition, toPosition );
            }

            public override int ItemCount => items.Count;


            private Action< int >? itemClicked;
            private bool addContextMenu;
        }
    }
}
