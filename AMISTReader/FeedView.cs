using Android.Content;
using Android.Graphics;
using Android.Views;
using AndroidX.Core.Content;
using AndroidX.RecyclerView.Widget;

namespace AMISTReader
{
    namespace NewsView
    {
        public class ItemHolder : RecyclerView.ViewHolder, View.IOnClickListener
        {
            public TextView? titleTV;
            public TextView? subtitleTV;

            public ItemHolder( View itemView, Action< int >? itemClicked ) : base( itemView )
            {
                titleTV     = itemView.FindViewById< TextView >( Resource.Id.FeedTitle );
                subtitleTV  = itemView.FindViewById< TextView >( Resource.Id.FeedSubtitle );

                this.itemClicked = itemClicked;
                if( this.itemClicked != null )
                    itemView.SetOnClickListener( this );
            }

            public void OnClick( View? v )
            {
                if( itemClicked != null )
                    itemClicked( BindingAdapterPosition );
            }


            private Action< int >? itemClicked;
        }

        public class Adapter : RecyclerView.Adapter
        {
            public Context parentContext;
            public List< FeedItem > items;
            public int selectedIndex;

            public Adapter( Context context, List< FeedItem > newItems, Action< int >? itemClicked )
            {
                parentContext = context;
                items = newItems;
                this.itemClicked = itemClicked;
            }

            public override RecyclerView.ViewHolder OnCreateViewHolder( ViewGroup parent, int viewType )
            {
                LayoutInflater? layoutInflater = LayoutInflater.From( parent.Context );
                View? itemView = layoutInflater?.Inflate( Resource.Layout.FeedItem, parent, false );

                if( itemView == null )
                    itemView = new View( null );

                return new ItemHolder( itemView, itemClicked );
            }

            public override void OnBindViewHolder( RecyclerView.ViewHolder holder, int position )
            {
                ItemHolder? itemHolder = ( ItemHolder ) holder;
                itemHolder?.titleTV?.Text = items[ position ].title;
                itemHolder?.subtitleTV?.Text = items[ position ].subtitle;

                if( items[ position ].hasBeenRead )
                    itemHolder?.titleTV?.SetTextColor( new Color( ContextCompat.GetColor( parentContext, Resource.Color.TextLightGrey ) ) );
                else
                    itemHolder?.titleTV?.SetTextColor( new Color( ContextCompat.GetColor( parentContext, Resource.Color.TextWhite ) ) );
            }

            public void OnItemMove( int fromPosition, int toPosition )
            {
                FeedItem swap = items[ fromPosition ];
                items.RemoveAt( fromPosition );
                items.Insert( toPosition, swap );
                NotifyItemMoved( fromPosition, toPosition );
            }

            public override int ItemCount => items.Count;


            private Action< int >? itemClicked;
        }
    }
}
