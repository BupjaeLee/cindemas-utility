package bupjae.android.cindemasutility;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

import com.androidquery.AQuery;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class IdolSelectActivity extends Activity {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = IdolSelectActivity.class.getSimpleName();
    private static final String ACTION_FILTER_CHANGED = "bupjae.android.cindemasutility.action.FILTER_CHANGED";
    private static final String EXTRA_FILTER_CONFIG = "bupjae.android.cindemasutility.extra.FILTER_CONFIG";
    public static final String EXTRA_CARD_ID = "bupjae.android.cindemasutility.extra.CARD_ID";

    private AQuery aq;
    private CursorAdapter adapter;

    private String query;
    private String sortOrder;
    private String filterRarity;
    private String filterAttribute;
    private ArrayList<Integer> filterConfig;

    private LoaderManager.LoaderCallbacks<Cursor> cursorCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case 0:
                    if (query == null) query = "";
                    if (sortOrder == null) sortOrder = "";
                    if (filterRarity == null) filterRarity = "";
                    if (filterAttribute == null) filterAttribute = "";
                    return new CursorLoader(
                            IdolSelectActivity.this,
                            Uri.parse("content://bupjae.android.cindemasutility.card/base"),
                            new String[]{"*", "card_id AS _id"},
                            "card_name GLOB ?" + filterRarity + filterAttribute,
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

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            filterConfig = intent.getIntegerArrayListExtra(EXTRA_FILTER_CONFIG);
            if (filterConfig == null) filterConfig = new ArrayList<>();

            if (filterConfig.contains(R.id.filter_attribute_cute))
                filterAttribute = " AND attribute = '큐트'";
            else if (filterConfig.contains(R.id.filter_attribute_cool))
                filterAttribute = " AND attribute = '쿨'";
            else if (filterConfig.contains(R.id.filter_attribute_passion))
                filterAttribute = " AND attribute = '패션'";
            else filterAttribute = "";

            StringBuilder builder = new StringBuilder();
            if (filterConfig.contains(R.id.filter_rarity_1)) builder.append(", 'N'");
            if (filterConfig.contains(R.id.filter_rarity_2)) builder.append(", 'N+'");
            if (filterConfig.contains(R.id.filter_rarity_3)) builder.append(", 'R'");
            if (filterConfig.contains(R.id.filter_rarity_4)) builder.append(", 'R+'");
            if (filterConfig.contains(R.id.filter_rarity_5)) builder.append(", 'SR'");
            if (filterConfig.contains(R.id.filter_rarity_6)) builder.append(", 'SR+'");
            filterRarity = builder.length() == 0 ? "" : String.format(" AND rarity IN (%s)", builder.substring(2));

            getLoaderManager().restartLoader(0, null, cursorCallback);
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
    protected void onResume() {
        super.onResume();

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        if (now.get(Calendar.MONTH) == Calendar.MARCH && now.get(Calendar.DAY_OF_MONTH) == 25) {
            SharedPreferences preference = getPreferences(MODE_PRIVATE);
            int current = now.get(Calendar.YEAR);
            int last = preference.getInt("easteregg_yayoi_last", 0);
            if (last < current) {
                preference.edit().putInt("easteregg_yayoi_last", current).apply();
                startActivity(new Intent(this, EasterEggYayoiActivity.class));
            }
        }
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
                sortOrder = "";
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
            case R.id.filter:
                onFilterClicked();
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
            intent.setClass(this, IdolProfileActivity.class);
            startActivity(intent);
        } else {
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setTitle(R.string.title_idol_select);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FILTER_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private void onFilterClicked() {
        FilterDialogFragment fragment = new FilterDialogFragment();
        if (filterConfig != null) {
            Bundle args = new Bundle();
            args.putIntegerArrayList(EXTRA_FILTER_CONFIG, filterConfig);
            fragment.setArguments(args);
        }
        fragment.show(getFragmentManager(), "dialog");
    }

    public static class FilterDialogFragment extends DialogFragment {
        private AQuery aq;
        private CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) return;
                int id = buttonView.getId();
                if (id != R.id.filter_attribute_all)
                    aq.id(R.id.filter_attribute_all).checked(false);
                if (id != R.id.filter_attribute_cute)
                    aq.id(R.id.filter_attribute_cute).checked(false);
                if (id != R.id.filter_attribute_cool)
                    aq.id(R.id.filter_attribute_cool).checked(false);
                if (id != R.id.filter_attribute_passion)
                    aq.id(R.id.filter_attribute_passion).checked(false);
            }
        };

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            @SuppressLint("InflateParams")
            View content = inflater.inflate(R.layout.dialog_idol_filter, null);
            aq = new AQuery(content);
            Class[] signature = new Class[]{CompoundButton.OnCheckedChangeListener.class};
            aq.id(R.id.filter_attribute_all).invoke("setOnCheckedChangeListener", signature, checkedChangeListener);
            aq.id(R.id.filter_attribute_cute).invoke("setOnCheckedChangeListener", signature, checkedChangeListener);
            aq.id(R.id.filter_attribute_cool).invoke("setOnCheckedChangeListener", signature, checkedChangeListener);
            aq.id(R.id.filter_attribute_passion).invoke("setOnCheckedChangeListener", signature, checkedChangeListener);
            fromArguments();
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.filter)
                    .setView(content)
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(ACTION_FILTER_CHANGED);
                            intent.putExtras(toBundle());
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                        }
                    })
                    .create();
        }

        private void fromArguments() {
            aq.id(R.id.filter_attribute_all).checked(true);
            if (getArguments() == null) return;
            List<Integer> idList = getArguments().getIntegerArrayList(EXTRA_FILTER_CONFIG);
            if (idList == null) return;
            for (int id : idList) {
                aq.id(id).checked(true);
            }
        }

        private Bundle toBundle() {
            class Check {
                private ArrayList<Integer> idList = new ArrayList<>();

                Check check(int id) {
                    if (aq.id(id).isChecked()) idList.add(id);
                    return this;
                }

                Bundle last() {
                    Bundle ret = new Bundle();
                    ret.putIntegerArrayList(EXTRA_FILTER_CONFIG, idList);
                    return ret;
                }
            }
            return new Check()
                    .check(R.id.filter_attribute_all)
                    .check(R.id.filter_attribute_cute)
                    .check(R.id.filter_attribute_cool)
                    .check(R.id.filter_attribute_passion)
                    .check(R.id.filter_rarity_1)
                    .check(R.id.filter_rarity_2)
                    .check(R.id.filter_rarity_3)
                    .check(R.id.filter_rarity_4)
                    .check(R.id.filter_rarity_5)
                    .check(R.id.filter_rarity_6)
                    .last();
        }
    }
}
