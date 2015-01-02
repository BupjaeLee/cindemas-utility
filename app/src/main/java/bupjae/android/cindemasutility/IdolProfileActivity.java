package bupjae.android.cindemasutility;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;

import com.androidquery.AQuery;

public class IdolProfileActivity extends ActionBarActivity {
    private AQuery aq;
    private ContentValues cardData;
    private CursorAdapter commentsAdaptor;

    private static String rarityToString(int rarity) {
        switch (rarity) {
            case 1:
                return "N";
            case 2:
                return "N+";
            case 3:
                return "R";
            case 4:
                return "R+";
            case 5:
                return "SR";
            case 6:
                return "SR+";
            default:
                return "-";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idol_profile);
        aq = new AQuery(this);

        commentsAdaptor = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                null,
                new String[]{"comments_value", "comments_key"},
                new int[]{android.R.id.text1, android.R.id.text2},
                0);
        aq.id(R.id.comment_list).adapter(commentsAdaptor);

        Bundle bundle = new Bundle();
        bundle.putLong(IdolSelectActivity.CARD_ID, getIntent().getLongExtra(IdolSelectActivity.CARD_ID, 0));
        getLoaderManager().initLoader(0, bundle, cursorCallback);
        getLoaderManager().initLoader(1, bundle, cursorCallback);
        getLoaderManager().initLoader(2, bundle, cursorCallback);
    }

    private LoaderManager.LoaderCallbacks<Cursor> cursorCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            long id = bundle.getLong(IdolSelectActivity.CARD_ID);
            switch (i) {
                case 0:
                    return new CursorLoader(IdolProfileActivity.this, Uri.parse("content://bupjae.android.cindemasutility.card/base/" + id), null, null, null, null);
                case 1:
                    return new CursorLoader(IdolProfileActivity.this, Uri.parse("content://bupjae.android.cindemasutility.card/evolve/" + id), null, null, null, null);
                case 2:
                    return new CursorLoader(IdolProfileActivity.this, Uri.parse("content://bupjae.android.cindemasutility.card/comments/" + id), null, null, null, null);
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            switch (cursorLoader.getId()) {
                case 0:
                    if (cursor.moveToNext()) {
                        cardData = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(cursor, cardData);

                        // TODO: use AQuery.image when AQuery supports ContentProvider URI
                        aq.id(R.id.info_icon).getImageView()
                                .setImageURI(Uri.parse("content://bupjae.android.cindemasutility.card/image/xs/" + cardData.getAsString("card_id")));
                        aq.id(R.id.info_name).text(cardData.getAsString("card_name"));
                        aq.id(R.id.info_rarity).text(rarityToString(cardData.getAsInteger("rarity")));
                        aq.id(R.id.info_cost).text(cardData.getAsString("cost"));
                        aq.id(R.id.info_maxlevel).text(cardData.getAsString("max_level"));
                    }
                    break;
                case 1:
                    if (cursor.moveToNext()) {
                        ContentValues evolveData = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(cursor, evolveData);

                        final long before = evolveData.getAsLong("evo_before_id");
                        aq.id(R.id.before_awake)
                                .visibility(before == 0 ? View.INVISIBLE : View.VISIBLE)
                                .text(R.string.before_awake, evolveData.getAsString("evo_before_name"))
                                .clicked(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Bundle bundle = new Bundle();
                                        bundle.putLong(IdolSelectActivity.CARD_ID, before);
                                        getLoaderManager().restartLoader(0, bundle, cursorCallback);
                                        getLoaderManager().restartLoader(1, bundle, cursorCallback);
                                        getLoaderManager().restartLoader(2, bundle, cursorCallback);
                                    }
                                });

                        final long after = evolveData.getAsLong("evo_after_id");
                        aq.id(R.id.after_awake)
                                .visibility(after == 0 ? View.INVISIBLE : View.VISIBLE)
                                .text(R.string.after_awake, evolveData.getAsString("evo_after_name"))
                                .clicked(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Bundle bundle = new Bundle();
                                        bundle.putLong(IdolSelectActivity.CARD_ID, after);
                                        getLoaderManager().restartLoader(0, bundle, cursorCallback);
                                        getLoaderManager().restartLoader(1, bundle, cursorCallback);
                                        getLoaderManager().restartLoader(2, bundle, cursorCallback);
                                    }
                                });
                    }
                    break;
                case 2:
                    commentsAdaptor.swapCursor(cursor);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            switch (cursorLoader.getId()) {
                case 2:
                    commentsAdaptor.swapCursor(null);
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
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

    public void onIconClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("content://bupjae.android.cindemasutility.card/image/l/" + cardData.getAsString("card_id"));
        intent.setDataAndType(uri, getContentResolver().getType(uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }


}
