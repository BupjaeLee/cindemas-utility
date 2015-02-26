package bupjae.android.cindemasutility;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

import com.androidquery.AQuery;

public class IdolSelectActivity extends Activity {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = IdolSelectActivity.class.getSimpleName();
    public static final String EXTRA_CARD_ID = "bupjae.android.cindemasutility.extra.CARD_ID";

    private AQuery aq;
    private CursorAdapter adapter;
    private String query;
    private String sortOrder;

    private LoaderManager.LoaderCallbacks<Cursor> cursorCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case 0:
                    if (query == null) query = "";
                    if (sortOrder == null) sortOrder = "card_id ASC";
                    return new CursorLoader(
                            IdolSelectActivity.this,
                            Uri.parse("content://bupjae.android.cindemasutility.card/base"),
                            new String[]{"*", "card_id AS _id"},
                            "card_name GLOB ?",
                            new String[]{"*" + query + "*"},
                            sortOrder);
                default:
                    throw new IllegalArgumentException("Wrong ID: " + i);
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            switch (cursorLoader.getId()) {
                case 0:
                    if (adapter != null && cursor != null) {
                        adapter.swapCursor(cursor);
                        aq.id(R.id.waiting_view).invisible();
                        aq.id(R.id.list_view).visible();
                    }
                    return;
                default:
                    throw new IllegalArgumentException("Wrong ID: " + cursorLoader.getId());
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            switch (cursorLoader.getId()) {
                case 0:
                    if (adapter != null) adapter.swapCursor(null);
                    return;
                default:
                    throw new IllegalArgumentException("Wrong ID: " + cursorLoader.getId());
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idol_select);
        aq = new AQuery(this);

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.listitem_idol_select,
                null,
                new String[]{
                        "card_name", "attribute", "rarity", "cost", "icon_uri",
                        "max_attack", "rate_attack", "max_defense", "rate_defense"
                },
                new int[]{
                        R.id.info_name, R.id.info_attribute, R.id.info_rarity, R.id.info_cost, R.id.info_icon,
                        R.id.info_max_attack, R.id.info_rate_attack, R.id.info_max_defense, R.id.info_rate_defense
                },
                0);
        aq.id(R.id.list_view).adapter(adapter).itemClicked(this, "onIdolSelected");

        getLoaderManager().initLoader(0, null, cursorCallback);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_idol_select, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setQueryHint(getResources().getString(R.string.search_hint));
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, "Test");
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                s = s.trim();
                query = s;
                getLoaderManager().restartLoader(0, null, cursorCallback);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                s = s.trim();
                query = s;
                getLoaderManager().restartLoader(0, null, cursorCallback);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sortby_database:
                item.setChecked(true);
                sortOrder = "card_id ASC";
                getLoaderManager().restartLoader(0, null, cursorCallback);
                return true;
            case R.id.sortby_atk:
                item.setChecked(true);
                sortOrder = "max_attack DESC";
                getLoaderManager().restartLoader(0, null, cursorCallback);
                return true;
            case R.id.sortby_def:
                item.setChecked(true);
                sortOrder = "max_defense DESC";
                getLoaderManager().restartLoader(0, null, cursorCallback);
                return true;
            case R.id.sortby_atkrate:
                item.setChecked(true);
                sortOrder = "rate_attack DESC";
                getLoaderManager().restartLoader(0, null, cursorCallback);
                return true;
            case R.id.sortby_defrate:
                item.setChecked(true);
                sortOrder = "rate_defense DESC";
                getLoaderManager().restartLoader(0, null, cursorCallback);
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }

    }

    @SuppressWarnings("UnusedDeclaration")
    public void onIdolSelected(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CARD_ID, id);
        if (getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            intent.setClass(IdolSelectActivity.this, IdolProfileActivity.class);
            startActivity(intent);
        } else {
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }
}
