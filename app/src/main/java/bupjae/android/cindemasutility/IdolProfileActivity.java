package bupjae.android.cindemasutility;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.androidquery.AQuery;

import java.util.HashMap;
import java.util.Map;

public class IdolProfileActivity extends Activity {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = IdolProfileActivity.class.getSimpleName();
    public static final String ACTION_REQUEST_CARD_ID = "bupjae.android.cindemasutility.action.REQUEST_CARD_ID";
    public static final String ACTION_CARD_ID_CHANGED = "bupjae.android.cindemasutility.action.CARD_ID_CHANGED";
    public static final String EXTRA_CARD_ID = IdolSelectActivity.EXTRA_CARD_ID;

    private AQuery aq;

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
                        ActionBar actionBar = getActionBar();
                        if (actionBar != null) actionBar.setTitle(cursor.getString(0));
                    }
                    break;
                case 1:
                    if (cursor.moveToFirst()) {
                        ContentValues evolveData = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(cursor, evolveData);
                        evolveFrom = evolveData.getAsLong("evo_before_id");
                        evolveTo = evolveData.getAsLong("evo_after_id");
                        aq.id(R.id.before_evolve).visibility(evolveFrom == 0 ? View.INVISIBLE : View.VISIBLE);
                        aq.id(R.id.after_evolve).visibility(evolveTo == 0 ? View.INVISIBLE : View.VISIBLE);
                    }
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            Intent intent = new Intent(ACTION_CARD_ID_CHANGED);
            intent.putExtra(EXTRA_CARD_ID, cardId);
            LocalBroadcastManager.getInstance(IdolProfileActivity.this).sendBroadcast(intent);
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

        aq.id(R.id.before_evolve).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (evolveFrom != 0)
                    changeCard(evolveFrom);
            }
        });
        aq.id(R.id.after_evolve).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (evolveTo != 0) changeCard(evolveTo);
            }
        });

        aq.id(R.id.info_tabs).invoke("setAdapter", new Class<?>[]{PagerAdapter.class}, new FragmentPagerAdapter(getFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                Bundle arg = new Bundle();
                switch (position) {
                    case 0:
                        return Fragment.instantiate(IdolProfileActivity.this, CardImageFragment.class.getName(), arg);
                    case 1:
                        return Fragment.instantiate(IdolProfileActivity.this, BasicProfileFragment.class.getName(), arg);
                    case 2:
                        return Fragment.instantiate(IdolProfileActivity.this, CommentFragment.class.getName(), arg);
                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "Image";
                    case 1:
                        return "Basic";
                    case 2:
                        return "Comments";
                    default:
                        return super.getPageTitle(position);
                }
            }
        });

        changeCard(getIntent().getLongExtra(EXTRA_CARD_ID, 0));
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REQUEST_CARD_ID);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    public abstract static class AbstractProfileFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
        private AQuery aq;
        private long cardId;

        private BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long curCardId = intent.getLongExtra(EXTRA_CARD_ID, 0);
                if (cardId != curCardId) {
                    cardId = curCardId;
                    getLoaderManager().restartLoader(0, null, AbstractProfileFragment.this);
                }
            }
        };

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(getLayoutId(), container, false);
            aq = new AQuery(root);
            return root;
        }

        @Override
        public void onStart() {
            super.onStart();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_CARD_ID_CHANGED);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

            Intent intent = new Intent(ACTION_REQUEST_CARD_ID);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }

        @Override
        public void onStop() {
            super.onStop();
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        }

        protected final AQuery aq(int id) {
            return aq.id(id);
        }

        protected final long cardId() {
            return cardId;
        }

        protected abstract int getLayoutId();
    }

    public static class CardImageFragment extends AbstractProfileFragment {
        private ImageType currentType;
        private ContentValues uriData;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            aq(R.id.image_framed).clicked(this, "onChangeImageType");
            aq(R.id.image_noframed).clicked(this, "onChangeImageType");
            aq(R.id.image_export).clicked(this, "onImageExport");
        }

        @Override
        protected int getLayoutId() {
            return R.layout.fragment_idol_card_image;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void onChangeImageType(View view) {
            currentType = ImageType.fromWidgetId(view.getId());
            updateImage();
        }

        @SuppressWarnings("UnusedDeclaration")
        public void onImageExport(View view) {
            if (uriData == null) return;
            String uriString = uriData.getAsString(currentType.toString());
            if (uriString == null) return;
            Uri source = Uri.parse(uriString);
            Bundle result = getActivity().getContentResolver().call(Uri.parse("content://" + ImageProvider.AUTHORITY), "export", source.toString(), null);
            if (result == null) {
                Toast.makeText(getActivity(), "Export error", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(result.<Uri>getParcelable(ImageProvider.EXTRA_EXPORTED_URI), result.getString(ImageProvider.EXTRA_EXPORTED_TYPE));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getActivity().startActivity(intent);
        }

        private void updateImage() {
            SharedPreferences preference = getActivity().getPreferences(MODE_PRIVATE);
            if (currentType == null) {
                try {
                    currentType = ImageType.valueOf(preference.getString("image_type", ImageType.FRAMED_CARD_IMAGE.toString()));
                } catch (IllegalArgumentException ex) {
                    currentType = ImageType.FRAMED_CARD_IMAGE;
                }
            }
            preference.edit().putString("image_type", currentType.toString()).apply();
            aq(currentType.getWidgetId()).checked(true);
            if (uriData == null) return;
            String uriString = uriData.getAsString(currentType.toString());
            if (uriString == null) return;
            aq(R.id.info_card_image).getImageView().setImageURI(Uri.parse(uriString));
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), Uri.parse("content://bupjae.android.cindemasutility.card/imageuri/" + cardId()), null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

            if (cursor.moveToFirst()) {
                uriData = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursor, uriData);
                updateImage();
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }

        public enum ImageType {
            FRAMED_CARD_IMAGE(R.id.image_framed),
            NOFRAMED_CARD_IMAGE(R.id.image_noframed);

            private static final Map<Integer, ImageType> fromWidgetIdMap = new HashMap<>();

            private final int widgetId;

            private ImageType(int widgetId) {
                this.widgetId = widgetId;
            }

            public int getWidgetId() {
                return widgetId;
            }

            public static ImageType fromWidgetId(int widgetId) {
                return fromWidgetIdMap.get(widgetId);
            }

            static {
                for (ImageType type : values()) {
                    fromWidgetIdMap.put(type.widgetId, type);
                }
            }
        }
    }

    public static class BasicProfileFragment extends AbstractProfileFragment {

        @Override
        protected int getLayoutId() {
            return R.layout.fragment_idol_basic_profile;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), Uri.parse("content://bupjae.android.cindemasutility.card/base/" + cardId()), null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor.moveToFirst()) {
                ContentValues cardData = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursor, cardData);
                aq(R.id.info_rarity).text(cardData.getAsString("rarity"));
                aq(R.id.info_cost).text(cardData.getAsString("cost"));
                aq(R.id.info_maxlevel).text(cardData.getAsString("max_level"));
                aq(R.id.info_max_attack).text(String.format("%d (%.2f)", cardData.getAsInteger("max_attack"), cardData.getAsDouble("rate_attack")));
                aq(R.id.info_max_defense).text(String.format("%d (%.2f)", cardData.getAsInteger("max_defense"), cardData.getAsDouble("rate_defense")));

                String skillName = cardData.getAsString("skill_name");
                if (skillName == null || skillName.isEmpty()) {
                    aq(R.id.info_skill_name).text("없음");
                    aq(R.id.info_skill_row).invisible();
                } else {
                    aq(R.id.info_skill_name).text(skillName);
                    aq(R.id.info_skill_effect).text(
                            String.format("%s (%d%%~)",
                                    cardData.getAsString("skill_effect"),
                                    cardData.getAsInteger("default_skill_effect")));
                    aq(R.id.info_skill_row).visible();
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    public static class CommentFragment extends AbstractProfileFragment {
        private CursorAdapter commentsAdaptor;

        private boolean showSecretComments;
        private String producerName;

        @Override
        protected int getLayoutId() {
            return R.layout.fragment_idol_comments;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            SharedPreferences preference = getActivity().getPreferences(MODE_PRIVATE);

            commentsAdaptor = new SimpleCursorAdapter(
                    getActivity(),
                    android.R.layout.simple_list_item_2,
                    null,
                    new String[]{"comments_value", "comments_kind"},
                    new int[]{android.R.id.text1, android.R.id.text2},
                    0);
            aq(R.id.comment_list).adapter(commentsAdaptor);
            aq(R.id.producer_name)
                    .text(producerName = preference.getString("producer_name", ""))
                    .textChanged(this, "onProducerNameChanged");
            aq(R.id.show_secret_comments)
                    .checked(showSecretComments = preference.getBoolean("show_secret_comments", false))
                    .clicked(this, "onShowSecretClicked");

            getLoaderManager().restartLoader(0, null, this);
        }

        @SuppressWarnings("UnusedDeclaration")
        public void onShowSecretClicked(View view) {
            showSecretComments = aq(R.id.show_secret_comments).isChecked();
            getActivity().getPreferences(MODE_PRIVATE).edit().putBoolean("show_secret_comments", showSecretComments).apply();
            getLoaderManager().restartLoader(0, null, this);
        }

        @SuppressWarnings("UnusedDeclaration")
        public void onProducerNameChanged(CharSequence s, int start, int before, int count) {
            producerName = aq(R.id.producer_name).getText().toString();
            getActivity().getPreferences(MODE_PRIVATE).edit().putString("producer_name", producerName).apply();
            getLoaderManager().restartLoader(0, null, this);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String pn = producerName == null || producerName.isEmpty() ? "(프로듀서)" : producerName;
            char trail = pn.charAt(pn.length() - 1);
            String atA = "이/가", atB = "은/는", atC = "을/를", atD = "와/과";
            if (0xac00 <= trail && trail <= 0xd7a3) {
                if ((trail - 0xac00) % 28 == 0) {
                    atA = "가";
                    atB = "는";
                    atC = "를";
                    atD = "와";
                } else {
                    atA = "이";
                    atB = "은";
                    atC = "을";
                    atD = "과";
                }
            }

            return new CursorLoader(
                    getActivity(),
                    Uri.parse("content://bupjae.android.cindemasutility.card/comments/" + cardId()),
                    new String[]{"kind_id AS _id", "comments_kind", "REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(comments_value, '%s', ?), '@a', ?), '@b', ?), '@c', ?), '@d', ?) AS comments_value"},
                    showSecretComments ? null : "secret = 0",
                    new String[]{pn, atA, atB, atC, atD},
                    null);
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
