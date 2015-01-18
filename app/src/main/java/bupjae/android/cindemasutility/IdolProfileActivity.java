package bupjae.android.cindemasutility;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;

import com.androidquery.AQuery;

public class IdolProfileActivity extends ActionBarActivity {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = IdolProfileActivity.class.getSimpleName();
    public static final String ACTION_CARD_ID_CHANGED = "bupjae.android.cindemasutility.action.CARD_ID_CHANGED";
    public static final String EXTRA_CARD_ID = IdolSelectActivity.EXTRA_CARD_ID;

    private AQuery aq;
    private Menu menu;

    private long cardId;
    private long evolveFrom;
    private long evolveTo;

    private LoaderManager.LoaderCallbacks<Cursor> cursorCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case 0:
                    return new CursorLoader(IdolProfileActivity.this, Uri.parse("content://bupjae.android.cindemasutility.card/base/" + cardId), new String[]{"card_name"}, null, null, null);
                case 1:
                    return new CursorLoader(IdolProfileActivity.this, Uri.parse("content://bupjae.android.cindemasutility.card/evolve/" + cardId), null, null, null, null);
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            switch (cursorLoader.getId()) {
                case 0:
                    if (cursor.moveToFirst()) {
                        aq.id(R.id.info_name).text(cursor.getString(0));
                    }
                    break;
                case 1:
                    if (cursor.moveToFirst()) {
                        evolveFrom = cursor.getLong(cursor.getColumnIndexOrThrow("evo_before_id"));
                        evolveTo = cursor.getLong(cursor.getColumnIndexOrThrow("evo_after_id"));
                        onPrepareOptionsMenu(menu);
                    }
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    };

    private void changeCard(long cardId) {
        if (cardId == 0) return;
        this.cardId = cardId;
        LoaderManager manager = getLoaderManager();
        for (int i = 0; i < 2; i++) {
            manager.restartLoader(i, null, cursorCallback);
        }

        Intent intent = new Intent(ACTION_CARD_ID_CHANGED);
        intent.putExtra(EXTRA_CARD_ID, cardId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idol_profile);
        aq = new AQuery(this);

        aq.id(R.id.info_tabs).invoke("setAdapter", new Class<?>[]{PagerAdapter.class}, new FragmentPagerAdapter(getFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                Bundle arg = new Bundle();
                arg.putLong(EXTRA_CARD_ID, cardId);
                switch (position) {
                    case 0:
                        return Fragment.instantiate(IdolProfileActivity.this, BasicProfileFragment.class.getName(), arg);
                    case 1:
                        return Fragment.instantiate(IdolProfileActivity.this, CommentFragment.class.getName(), arg);
                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "Basic";
                    case 1:
                        return "Comments";
                    default:
                        return super.getPageTitle(position);
                }
            }
        });

        changeCard(getIntent().getLongExtra(EXTRA_CARD_ID, 0));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_idol_profile, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (menu == null) return true;
        menu.findItem(R.id.action_before_awake).setEnabled(evolveFrom != 0);
        menu.findItem(R.id.action_after_awake).setEnabled(evolveTo != 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_before_awake) {
            changeCard(evolveFrom);
            return true;
        }
        if (id == R.id.action_after_awake) {
            changeCard(evolveTo);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class BasicProfileFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
        private AQuery aq;

        private long cardId;

        private BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                cardId = intent.getLongExtra(EXTRA_CARD_ID, 0);
                getLoaderManager().restartLoader(0, null, BasicProfileFragment.this);
            }
        };

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_idol_basic_profile, container, false);
            aq = new AQuery(root);
            if (savedInstanceState != null) {
                cardId = savedInstanceState.getLong(EXTRA_CARD_ID);
            } else {
                cardId = getArguments().getLong(EXTRA_CARD_ID);
            }
            return root;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            getLoaderManager().restartLoader(0, null, this);
        }


        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), Uri.parse("content://bupjae.android.cindemasutility.card/base/" + cardId), null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor.moveToFirst()) {
                ContentValues cardData = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursor, cardData);
                aq.id(R.id.info_card_image).getImageView().setImageURI(Uri.parse(cardData.getAsString("image_uri")));
                aq.id(R.id.info_rarity).text(cardData.getAsString("rarity"));
                aq.id(R.id.info_cost).text(cardData.getAsString("cost"));
                aq.id(R.id.info_maxlevel).text(cardData.getAsString("max_level"));
                aq.id(R.id.info_maxattack).text(String.format("%d (%.1f)", cardData.getAsInteger("max_attack"), cardData.getAsDouble("rate_attack")));
                aq.id(R.id.info_maxdefense).text(String.format("%d (%.1f)", cardData.getAsInteger("max_defense"), cardData.getAsDouble("rate_defense")));
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_CARD_ID_CHANGED);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
        }

        @Override
        public void onStop() {
            super.onStop();
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putLong(EXTRA_CARD_ID, cardId);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    public static class CommentFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
        private static final String SHOW_SECRET_COMMENTS = "bupjae.android.cindemasutility.SHOW_SECRET_COMMENTS";

        private AQuery aq;
        private CursorAdapter commentsAdaptor;

        private long cardId;
        private boolean showSecretComments;

        private BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                cardId = intent.getLongExtra(EXTRA_CARD_ID, 0);
                getLoaderManager().restartLoader(0, null, CommentFragment.this);
            }
        };

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_idol_comments, container, false);
            aq = new AQuery(root);
            if (savedInstanceState != null) {
                cardId = savedInstanceState.getLong(EXTRA_CARD_ID);
                showSecretComments = savedInstanceState.getBoolean(SHOW_SECRET_COMMENTS);
                aq.id(R.id.show_secret_comments).checked(showSecretComments);
            } else {
                cardId = getArguments().getLong(EXTRA_CARD_ID);
            }
            return root;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            commentsAdaptor = new SimpleCursorAdapter(
                    getActivity(),
                    android.R.layout.simple_list_item_2,
                    null,
                    new String[]{"comments_value", "comments_kind"},
                    new int[]{android.R.id.text1, android.R.id.text2},
                    0);
            aq.id(R.id.comment_list).adapter(commentsAdaptor);
            aq.id(R.id.show_secret_comments).clicked(this, "onShowSecretClicked");

            getLoaderManager().restartLoader(0, null, this);
        }

        @Override
        public void onStart() {
            super.onStart();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_CARD_ID_CHANGED);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
        }

        @Override
        public void onStop() {
            super.onStop();
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SHOW_SECRET_COMMENTS, showSecretComments);
            outState.putLong(EXTRA_CARD_ID, cardId);
        }

        @SuppressWarnings("UnusedDeclaration")
        public void onShowSecretClicked(View view) {
            showSecretComments = aq.id(R.id.show_secret_comments).isChecked();
            getLoaderManager().restartLoader(0, null, this);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(
                    getActivity(),
                    Uri.parse("content://bupjae.android.cindemasutility.card/comments/" + cardId),
                    new String[]{"*", "card_id*100+kind_id AS _id"},
                    showSecretComments ? null : "secret = 0",
                    null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            commentsAdaptor.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            commentsAdaptor.swapCursor(null);
        }
    }
}
