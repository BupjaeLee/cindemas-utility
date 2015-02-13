package bupjae.android.cindemasutility;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class CardProvider extends ContentProvider {
    // Image type: l, lc, lls, ls, m, s, xl, xs, xs3, xs4, xxs

    public static final String AUTHORITY = "bupjae.android.cindemasutility.card";

    private static final String TAG = CardProvider.class.getSimpleName();
    private static final String CARD_DB_FILENAME = "data/csv/card_data.db";
    private static final String CARD_COMPOSITION_FILENAME = "data/csv/composition.db";
    private static final String CARD_COMMENTS_FILENAME = "data/csv/card_comments.db";
    private static final String CARD_BIRTHDAY_FILENAME = "data/csv/idol_birthday.db";
    private static final String CARD_SKILLDATA_FILENAME = "data/csv/skill_data.db";
    private static final String CARD_VCOMMENT_FILENAME = "data/csv/v_comment.db";
    private static final String CARD_IMAGE_DIR = "images/card";

    private static final int CODE_BASE = 1;
    private static final int CODE_BASE_ID = 2;
    private static final int CODE_IMAGE = 3;
    private static final int CODE_EVOLVE = 4;
    private static final int CODE_EVOLVE_ID = 5;
    private static final int CODE_COMMENTS = 6;
    private static final int CODE_COMMENTS_ID = 7;
    private static final int CODE_STAT_ID = 8;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final String[] DEFAULT_OPENABLE_COLUMNS = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};

    private File baseDir;
    private SQLiteOpenHelper helper;

    static {
        uriMatcher.addURI(AUTHORITY, "base", CODE_BASE);
        uriMatcher.addURI(AUTHORITY, "base/#", CODE_BASE_ID);
        uriMatcher.addURI(AUTHORITY, "image/*/#", CODE_IMAGE);
        uriMatcher.addURI(AUTHORITY, "evolve", CODE_EVOLVE);
        uriMatcher.addURI(AUTHORITY, "evolve/#", CODE_EVOLVE_ID);
        uriMatcher.addURI(AUTHORITY, "comments", CODE_COMMENTS);
        uriMatcher.addURI(AUTHORITY, "comments/#", CODE_COMMENTS_ID);
        uriMatcher.addURI(AUTHORITY, "stat/#", CODE_STAT_ID);
    }

    private static String getCardImageType(Uri uri) {
        List<String> segments = uri.getPathSegments();
        return segments.get(segments.size() - 2);
    }

    private static String getCardImageId(Uri uri) {
        return uri.getLastPathSegment();
    }

    private static String getCardImageExtension(Uri uri) {
        return getCardImageType(uri).equals("xl") ? ".webp" : ".png";
    }

    private static byte[] readFully(File file) throws FileNotFoundException {
        InputStream fin = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try {
            while (true) {
                int n = fin.read(buf);
                if (n == -1) break;
                out.write(buf, 0, n);
            }
        } catch (IOException ex) {
            Log.e(TAG, "readFully(): reading failed", ex);
        }
        return out.toByteArray();
    }

    @Override
    public boolean onCreate() {
        baseDir = new File(Environment.getExternalStorageDirectory(), "kr.daum_mobage.am_db.g13001173/imas_cg_assets_android");
        helper = new SQLiteOpenHelper(getContext(), "db", null, 1) {
            private void initializeDatabase(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE stat (" +
                        "card_id INTEGER PRIMARY KEY, " +
                        "no_levelup INTEGER, " +
                        "min_start_attack INTEGER, " +
                        "max_start_attack INTEGER, " +
                        "min_final_attack INTEGER, " +
                        "max_final_attack INTEGER, " +
                        "min_start_defense INTEGER, " +
                        "max_start_defense INTEGER, " +
                        "min_final_defense INTEGER, " +
                        "max_final_defense INTEGER)");
                db.execSQL("INSERT INTO stat VALUES (0, 0, 0, 0, 0, 0, 0, 0, 0, 0)");
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                initializeDatabase(db);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS stat");
                initializeDatabase(db);
            }

            @Override
            public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS stat");
                initializeDatabase(db);
            }

            @Override
            public void onOpen(SQLiteDatabase db) {
                super.onOpen(db);
                db.execSQL("ATTACH DATABASE ? AS card_data", new Object[]{new File(baseDir, CARD_DB_FILENAME).toString()});
                db.execSQL("ATTACH DATABASE ? AS composition", new Object[]{new File(baseDir, CARD_COMPOSITION_FILENAME).toString()});
                db.execSQL("ATTACH DATABASE ? AS card_comments", new Object[]{new File(baseDir, CARD_COMMENTS_FILENAME).toString()});
                db.execSQL("ATTACH DATABASE ? AS idol_birthday", new Object[]{new File(baseDir, CARD_BIRTHDAY_FILENAME).toString()});
                db.execSQL("ATTACH DATABASE ? AS skill_data", new Object[]{new File(baseDir, CARD_SKILLDATA_FILENAME).toString()});
                try {
                    db.execSQL("ATTACH DATABASE ? AS v_comment", new Object[]{new File(baseDir, CARD_VCOMMENT_FILENAME).toString()});
                } catch (SQLException ex) {
                    db.execSQL("CREATE TEMP TABLE v_comment(card_id, card_name, v_comments)");
                }
                db.execSQL("CREATE TEMP VIEW base AS SELECT " +
                        "card_data.card_id AS card_id, " +
                        "card_data.card_name AS card_name, " +
                        "(CASE card_data.attribute " +
                        "  WHEN 1 THEN '큐트' " +
                        "  WHEN 2 THEN '쿨' " +
                        "  WHEN 3 THEN '패션' " +
                        "  ELSE '-' END) AS attribute, " +
                        "(CASE card_data.rarity " +
                        "  WHEN 1 THEN 'N' " +
                        "  WHEN 2 THEN 'N+' " +
                        "  WHEN 3 THEN 'R' " +
                        "  WHEN 4 THEN 'R+' " +
                        "  WHEN 5 THEN 'SR' " +
                        "  WHEN 6 THEN 'SR+' " +
                        "  ELSE '-' END) AS rarity, " +
                        "card_data.cost AS cost, " +
                        "card_data.max_level AS max_level, " +
                        "stat.max_final_attack AS max_attack, " +
                        "stat.max_final_defense AS max_defense, " +
                        "ROUND(CAST(stat.max_final_attack AS REAL) / CAST(card_data.cost AS REAL), 2) AS rate_attack, " +
                        "ROUND(CAST(stat.max_final_defense AS REAL) / CAST(card_data.cost AS REAL), 2) AS rate_defense, " +
                        "'content://bupjae.android.cindemasutility.card/image/xs/' || card_data.card_id AS icon_uri, " +
                        "'content://bupjae.android.cindemasutility.card/image/l/' || card_data.card_id AS image_uri, " +
                        "card_data.skill_name AS skill_name, " +
                        "REPLACE(card_data.skill_effect, '\\n', ' ') AS skill_effect, " +
                        "skill_data.default_skill_effect AS default_skill_effect " +
                        "FROM card_data " +
                        "LEFT JOIN stat ON card_data.card_id = stat.card_id " +
                        "LEFT JOIN skill_data ON card_data.skill_id = skill_data.skill_id");
                db.execSQL("CREATE TEMP VIEW evolve AS SELECT " +
                        "c1.card_id AS card_id, " +
                        "c1.evo_card_id AS evo_after_id, " +
                        "IFNULL(c2.card_id, 0) AS evo_before_id, " +
                        "IFNULL(c2.material_card, 0) AS material_id " +
                        "FROM composition AS c1 " +
                        "LEFT JOIN composition AS c2 ON c1.card_id = c2.evo_card_id " +
                        "LEFT JOIN composition AS c3 ON c1.evo_card_id = c3.card_id");
                db.execSQL("CREATE TEMP VIEW comments AS " +
                        "      SELECT card_id, 0 AS kind_id, 'profile' AS comments_kind, comment AS comments_value, 0 AS secret FROM card_data " +
                        "UNION SELECT card_id, 1 AS kind_id, 'my_1' AS comments_kind, comments_my_1 AS comments_value, 0 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 2 AS kind_id, 'my_2' AS comments_kind, comments_my_2 AS comments_value, 0 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 3 AS kind_id, 'my_3' AS comments_kind, comments_my_3 AS comments_value, 0 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 4 AS kind_id, 'my_4' AS comments_kind, comments_my_4 AS comments_value, 0 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 5 AS kind_id, 'my_max' AS comments_kind, comments_my_max AS comments_value, 1 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 6 AS kind_id, 'work_1' AS comments_kind, comments_work_1 AS comments_value, 0 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 7 AS kind_id, 'work_2' AS comments_kind, comments_work_2 AS comments_value, 0 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 8 AS kind_id, 'work_3' AS comments_kind, comments_work_3 AS comments_value, 0 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 9 AS kind_id, 'work_4' AS comments_kind, comments_work_4 AS comments_value, 0 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 10 AS kind_id, 'work_max' AS comments_kind, comments_work_max AS comments_value, 1 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 11 AS kind_id, 'work_love_up' AS comments_kind, comments_work_love_up AS comments_value, 1 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 12 AS kind_id, 'live' AS comments_kind, comments_live AS comments_value, 0 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 13 AS kind_id, 'love_max' AS comments_kind, comments_love_max AS comments_value, 1 AS secret FROM card_comments " +
                        "UNION SELECT card_id, 14 AS kind_id, 'birthday1' AS comments_kind, comment1 AS comment_value, 1 AS secret FROM idol_birthday " +
                        "UNION SELECT card_id, 15 AS kind_id, 'birthday2' AS comments_kind, comment2 AS comment_value, 1 AS secret FROM idol_birthday " +
                        "UNION SELECT card_id, 16 AS kind_id, 'birthday3' AS comments_kind, comment3 AS comment_value, 1 AS secret FROM idol_birthday " +
                        "UNION SELECT card_id, 17 AS kind_id, 'valentine' AS comments_kind, v_comments AS comment_value, 1 AS secret FROM v_comment");
                db.execSQL("CREATE TEMP VIEW current_stat AS SELECT " +
                        "card_id, " +
                        "max_level=1 AS no_levelup, " +
                        "default_attack AS start_attack, " +
                        "    (CASE WHEN max_level=1 THEN default_attack ELSE max_attack END) + " +
                        "    (CASE WHEN max_love=0 THEN 0 ELSE bonus_attack END) " +
                        "AS final_attack, " +
                        "default_defense AS start_defense, " +
                        "    (CASE WHEN max_level=1 THEN default_defense ELSE max_defense END) + " +
                        "    (CASE WHEN max_love=0 THEN 0 ELSE bonus_defense END) " +
                        "AS final_defense " +
                        "FROM card_data");
                db.execSQL("CREATE TEMP VIEW inherit_stat AS SELECT " +
                        "card_id, " +
                        "(CASE WHEN no_levelup THEN (min_start_attack+9)/10 ELSE (min_start_attack+19)/20 END) AS min_attack, " +
                        "(CASE WHEN no_levelup THEN (min_start_defense+9)/10 ELSE (min_start_defense+19)/20 END) AS min_defense, " +
                        "(max_final_attack+9)/10 AS max_attack," +
                        "(max_final_defense+9)/10 AS max_defense " +
                        "FROM stat");

                Cursor cursor = db.rawQuery("SELECT card_id FROM evolve WHERE evo_after_id=0 AND card_id NOT IN (SELECT card_id FROM stat)", null);
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    Log.d(TAG, "prepareStatData started");
                    while (cursor.moveToNext()) {
                        prepareStatData(db, cursor.getLong(0));
                    }
                    Log.d(TAG, "prepareStatData finished");
                } finally {
                    cursor.close();
                }
            }

            private void prepareStatData(SQLiteDatabase db, long id) {
                if (id == 0) return;

                String[] selection = new String[]{String.valueOf(id)};
                if (DatabaseUtils.queryNumEntries(db, "stat", "card_id=?", selection) > 0) return;

                long c1;
                long c2;
                Cursor cursor = db.rawQuery("SELECT evo_before_id, material_id FROM evolve WHERE card_id=?", selection);
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (!cursor.moveToNext()) {
                        Log.e(TAG, "prepareStatData: unexpected empty cursor: " + id);
                        return;
                    }
                    c1 = cursor.getLong(0);
                    c2 = cursor.getLong(1);
                } finally {
                    cursor.close();
                }
                prepareStatData(db, c1);
                prepareStatData(db, c2);

                db.execSQL("INSERT OR IGNORE INTO stat SELECT " +
                                "?1, no_levelup, " +
                                "start_attack+min_attack, start_attack+max_attack, " +
                                "final_attack+min_attack, final_attack+max_attack, " +
                                "start_defense+min_defense, start_defense+max_defense, " +
                                "final_defense+min_defense, final_defense+max_defense " +
                                "FROM " +
                                "(SELECT " +
                                "SUM(min_attack) AS min_attack, " +
                                "SUM(max_attack) AS max_attack, " +
                                "SUM(min_defense) AS min_defense, " +
                                "SUM(max_defense) AS max_defense " +
                                "FROM (" +
                                "SELECT * FROM inherit_stat WHERE card_id=?2 UNION ALL " +
                                "SELECT * FROM inherit_stat WHERE card_id=?3)), " +
                                "(SELECT * FROM current_stat WHERE card_id=?1)",
                        new Object[]{id, c1, c2});
            }
        };
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CODE_BASE:
                return "vnd.android.cursor.dir/vnd.bupjae.android.cindemasutility.card.base";
            case CODE_BASE_ID:
                return "vnd.android.cursor.item/vnd.bupjae.android.cindemasutility.card.base";
            case CODE_IMAGE:
                return getCardImageType(uri).equals("xl") ? "image/webp" : "image/png";
            case CODE_EVOLVE:
                return "vnd.android.cursor.dir/vnd.bupjae.android.cindemasutility.card.evolve";
            case CODE_EVOLVE_ID:
                return "vnd.android.cursor.item/vnd.bupjae.android.cindemasutility.card.evolve";
            case CODE_COMMENTS:
                return "vnd.android.cursor.dir/vnd.bupjae.android.cindemasutility.card.comments";
            case CODE_COMMENTS_ID:
                return "vnd.android.cursor.item/vnd.bupjae.android.cindemasutility.card.comments";
            case CODE_STAT_ID:
                return "vnd.android.cursor.item/vnd.bupjae.android.cindemasutility.card.stat";
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (selection == null || selection.trim().length() == 0) {
            selection = "1";
        }
        switch (uriMatcher.match(uri)) {
            case CODE_BASE_ID: {
                selection = "(" + selection + ") AND card_id = ?";
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
            }
            // FALL_THROUGH
            case CODE_BASE: {
                return helper.getReadableDatabase().query("base", projection, selection, selectionArgs, null, null, sortOrder);
            }
            case CODE_IMAGE: {
                if (!selection.equals("1")) {
                    Log.w(TAG, "Ignoring selection '" + selection + "' for image query");
                }
                String id = getCardImageId(uri);
                Log.d(TAG, "image projection :: " + Arrays.toString(projection));
                if (projection == null) {
                    projection = DEFAULT_OPENABLE_COLUMNS;
                }
                Object[] values = new Object[projection.length];
                for (int i = 0; i < projection.length; i++) {
                    switch (projection[i]) {
                        case OpenableColumns.DISPLAY_NAME:
                            values[i] = String.format("%s_%s.%s", getCardImageType(uri), id, getCardImageExtension(uri));
                            break;
                        case OpenableColumns.SIZE:
                            values[i] = getOriginalCardImageFile(uri).length();
                            break;
                    }
                }
                MatrixCursor cursor = new MatrixCursor(projection, 1);
                cursor.addRow(values);
                return cursor;
            }
            case CODE_EVOLVE_ID: {
                selection = "(" + selection + ") AND card_id = ?";
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
            }
            // FALL_THROUGH
            case CODE_EVOLVE: {
                return helper.getReadableDatabase().query("evolve", projection, selection, selectionArgs, null, null, sortOrder);
            }
            case CODE_COMMENTS_ID: {
                selection = "(" + selection + ") AND card_id = ?";
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
            }
            // FALL_THROUGH
            case CODE_COMMENTS: {
                return helper.getReadableDatabase().query("comments", projection, selection, selectionArgs, null, null, sortOrder);
            }
            case CODE_STAT_ID: {
                if (!selection.equals("1")) {
                    Log.w(TAG, "Ignoring selection '" + selection + "' for single row query");
                }
                selection = "card_id = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
                return helper.getReadableDatabase().query("stat", projection, selection, selectionArgs, null, null, sortOrder);
            }
            default:
                throw new IllegalArgumentException("Unknown URI : " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.w(TAG, "Cannot insert on read-only content provider");
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.w(TAG, "Cannot delete on read-only content provider");
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.w(TAG, "Cannot update on read-only content provider");
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!mode.equals("r")) {
            throw new FileNotFoundException("Unsupported mode: " + mode);
        }
        switch (uriMatcher.match(uri)) {
            case CODE_IMAGE:
                File cacheFile = getCachedCardImageFile(uri);
                if (!cacheFile.exists()) {
                    File file = getOriginalCardImageFile(uri);
                    byte[] buffer = readFully(file);
                    byte mask;
                    switch (getType(uri)) {
                        case "image/png":
                            mask = (byte) (buffer[0] ^ 137);
                            break;
                        case "image/webp":
                            mask = (byte) (buffer[0] ^ 'R');
                            break;
                        default:
                            mask = 0;
                            break;
                    }
                    for (int i = 0; i < 50; i++) buffer[i] ^= mask;
                    try {
                        OutputStream os = new FileOutputStream(cacheFile);
                        try {
                            os.write(buffer);
                        } finally {
                            try {
                                os.close();
                            } catch (IOException ex) {
                                Log.w(TAG, "openFile(): closing failed", ex);
                            }
                        }
                    } catch (IOException ex) {
                        Log.e(TAG, "openFile(): writing failed", ex);
                        return null;
                    }
                }
                return ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY);
            default:
                throw new FileNotFoundException(uri.toString());
        }
    }

    private File getOriginalCardImageFile(Uri uri) {
        List<String> segments = uri.getPathSegments();
        String type = segments.get(segments.size() - 2);
        String id = segments.get(segments.size() - 1);
        return new File(new File(new File(baseDir, CARD_IMAGE_DIR), type), id + (type.equals("xl") ? ".webp" : ".png"));
    }

    private File getCachedCardImageFile(Uri uri) {
        List<String> segments = uri.getPathSegments();
        String type = segments.get(segments.size() - 2);
        String id = segments.get(segments.size() - 1);
        return new File(getContext().getCacheDir(), String.format("%s_%s.%s", type, id, (type.equals("xl") ? ".webp" : ".png")));
    }
}
