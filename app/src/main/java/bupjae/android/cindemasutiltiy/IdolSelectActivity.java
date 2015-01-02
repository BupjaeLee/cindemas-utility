package bupjae.android.cindemasutiltiy;

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
    public static final String CARD_ID = "bupjae.android.cindemasutility.CARD_ID";

    private CursorAdapter adapter;

    private LoaderManager.LoaderCallbacks<Cursor> cursorCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case 0:
                    return new CursorLoader(IdolSelectActivity.this, Uri.parse("content://bupjae.android.cindemasutility.card/base"), null, null, null, null);
                default:
                    throw new IllegalArgumentException("Wrong ID: " + i);
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            switch (cursorLoader.getId()) {
                case 0:
                    if (adapter != null && cursor != null) adapter.swapCursor(cursor);
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
        AQuery aq = new AQuery(this);

        adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_1,
                null,
                new String[]{"card_name"},
                new int[]{android.R.id.text1},
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
        intent.putExtra(CARD_ID, id);
        if (getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            intent.setClass(IdolSelectActivity.this, IdolProfileActivity.class);
            startActivity(intent);
        } else {
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }
}
