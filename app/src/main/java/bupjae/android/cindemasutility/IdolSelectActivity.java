package bupjae.android.cindemasutility;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;

import com.androidquery.AQuery;

public class IdolSelectActivity extends ActionBarActivity {
    public static final String EXTRA_CARD_ID = "bupjae.android.cindemasutility.extra.CARD_ID";

    private AQuery aq;
    private CursorAdapter adapter;

    private LoaderManager.LoaderCallbacks<Cursor> cursorCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case 0:
                    return new CursorLoader(
                            IdolSelectActivity.this,
                            Uri.parse("content://bupjae.android.cindemasutility.card/base"),
                            new String[]{"*", "card_id AS _id"},
                            null, null, null);
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_idol_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
