package bupjae.android.cindemasutility;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class CardProvider extends ContentProvider {
    // http://sourceforge.net/projects/vgmtoolbox :: CRI-HCA extractor
    // http://www.mediafire.com/?858tqikj74qinw4 :: HCA decoder
    public static final String AUTHORITY = "bupjae.android.cindemasutility.card";

    private static final String TAG = CardProvider.class.getSimpleName();
    private static final String CARD_DB_FILENAME = "data/csv/card_data.db";
    private static final String CARD_COMPOSITION_FILENAME = "data/csv/composition.db";
    private static final String CARD_COMMENTS_FILENAME = "data/csv/card_comments.db";
    private static final String CARD_BIRTHDAY_FILENAME = "data/csv/idol_birthday.db";
    private static final String CARD_SKILLDATA_FILENAME = "data/csv/skill_data.db";
    private static final String CARD_VCOMMENT_FILENAME = "data/csv/v_comment.db";
    private static final String CARD_PROFILE_FILENAME = "data/csv/card_profile.db";
    private static final String CARD_GACHA_COMMENTS_FILENAME = "data/csv/gacha_sr_win_comments.db";

    private static final int CODE_BASE = 1;
    private static final int CODE_BASE_ID = 2;
    private static final int CODE_EVOLVE = 4;
    private static final int CODE_EVOLVE_ID = 5;
    private static final int CODE_COMMENTS = 6;
    private static final int CODE_COMMENTS_ID = 7;
    private static final int CODE_STAT_ID = 8;
    private static final int CODE_IMAGEURI_ID = 9;
    private static final int CODE_DETAIL = 10;
    private static final int CODE_DETAIL_ID = 11;
    private static final int CODE_CALCULATE_ID = 12;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private File baseDir;
    private SQLiteOpenHelper helper;

    static {
        uriMatcher.addURI(AUTHORITY, "base", CODE_BASE);
        uriMatcher.addURI(AUTHORITY, "base/#", CODE_BASE_ID);
        uriMatcher.addURI(AUTHORITY, "evolve", CODE_EVOLVE);
        uriMatcher.addURI(AUTHORITY, "evolve/#", CODE_EVOLVE_ID);
        uriMatcher.addURI(AUTHORITY, "comments", CODE_COMMENTS);
        uriMatcher.addURI(AUTHORITY, "comments/#", CODE_COMMENTS_ID);
        uriMatcher.addURI(AUTHORITY, "stat/#", CODE_STAT_ID);
        uriMatcher.addURI(AUTHORITY, "imageuri/#", CODE_IMAGEURI_ID);
        uriMatcher.addURI(AUTHORITY, "detail", CODE_DETAIL);
        uriMatcher.addURI(AUTHORITY, "detail/#", CODE_DETAIL_ID);
        uriMatcher.addURI(AUTHORITY, "calculate/#", CODE_CALCULATE_ID);
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
                db.execSQL("ATTACH DATABASE ? AS card_profile", new Object[]{new File(baseDir, CARD_PROFILE_FILENAME).toString()});
                db.execSQL("ATTACH DATABASE ? AS gacha_sr_win_comments", new Object[]{new File(baseDir, CARD_GACHA_COMMENTS_FILENAME).toString()});
                if (new File(baseDir, CARD_VCOMMENT_FILENAME).exists()) {
                    db.execSQL("ATTACH DATABASE ? AS v_comment", new Object[]{new File(baseDir, CARD_VCOMMENT_FILENAME).toString()});
                } else {
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
                        "'content://" + ImageProvider.AUTHORITY + "/card/xs/' || card_data.card_id || '.png' AS icon_uri, " +
                        "card_data.skill_name AS skill_name, " +
                        "REPLACE(card_data.skill_effect, '\\n', ' ') AS skill_effect, " +
                        "skill_data.default_skill_effect AS default_skill_effect, " +
                        "skill_data.max_skill_effect AS max_skill_effect " +
                        "FROM card_data " +
                        "LEFT JOIN stat ON card_data.card_id = stat.card_id " +
                        "LEFT JOIN skill_data ON card_data.skill_id = skill_data.skill_id");
                db.execSQL("CREATE TEMP VIEW calculate AS SELECT " +
                        "card_data.card_id AS card_id, " +
                        "card_data.card_name AS card_name, " +
                        "card_data.attribute AS attribute, " +
                        "card_data.cost AS cost, " +
                        "stat.max_final_attack AS attack, " +
                        "stat.max_final_defense AS defense, " +
                        "'content://" + ImageProvider.AUTHORITY + "/card/ls/' || card_data.card_id || '.png' AS image_uri, " +
                        "'content://" + ImageProvider.AUTHORITY + "/card/xs/' || card_data.card_id || '.png' AS icon_uri, " +
                        "skill_data.skill_name AS skill_name, " +
                        "REPLACE(skill_data.skill_effect, '\\n', ' ') AS skill_effect, " +
                        "(skill_data.skill_conditions IN (1, 3)) AS attack_skill, " +
                        "(skill_data.skill_conditions IN (2, 3)) AS defense_skill, " +
                        "skill_data.skill_target AS skill_target, " +
                        "skill_data.level1_skill_effect AS skill_effect_1, " +
                        "skill_data.level2_skill_effect AS skill_effect_2, " +
                        "skill_data.level3_skill_effect AS skill_effect_3, " +
                        "skill_data.level4_skill_effect AS skill_effect_4, " +
                        "skill_data.level5_skill_effect AS skill_effect_5, " +
                        "skill_data.level6_skill_effect AS skill_effect_6, " +
                        "skill_data.level7_skill_effect AS skill_effect_7, " +
                        "skill_data.level8_skill_effect AS skill_effect_8, " +
                        "skill_data.level9_skill_effect AS skill_effect_9, " +
                        "skill_data.level10_skill_effect AS skill_effect_10 " +
                        "FROM card_data " +
                        "LEFT JOIN stat ON card_data.card_id = stat.card_id " +
                        "LEFT JOIN skill_data ON card_data.skill_id = skill_data.skill_id");
                db.execSQL("CREATE TEMP VIEW evolve AS SELECT " +
                        "c1.card_id AS card_id, " +
                        "c1.evo_card_id AS evo_after_id, " +
                        "IFNULL(d1.card_name, '') AS evo_after_name, " +
                        "IFNULL(c2.card_id, 0) AS evo_before_id, " +
                        "IFNULL(d2.card_name, '') AS evo_before_name, " +
                        "IFNULL(c2.material_card, 0) AS material_id " +
                        "FROM composition AS c1 " +
                        "LEFT JOIN composition AS c2 ON c1.card_id = c2.evo_card_id " +
                        "LEFT JOIN card_data AS d1 ON c1.evo_card_id = d1.card_id " +
                        "LEFT JOIN card_data AS d2 ON c2.card_id = d2.card_id");
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
                        "UNION SELECT card_id, 17 AS kind_id, 'valentine' AS comments_kind, v_comments AS comment_value, 1 AS secret FROM v_comment " +
                        "UNION SELECT card_id, 18 AS kind_id, 'gacha1' AS comments_kind, message_1 AS comment_value, 1 AS secret FROM gacha_sr_win_comments " +
                        "UNION SELECT card_id, 19 AS kind_id, 'gacha2' AS comments_kind, message_2 AS comment_value, 1 AS secret FROM gacha_sr_win_comments " +
                        "UNION SELECT card_id, 20 AS kind_id, 'gacha3' AS comments_kind, message_3 AS comment_value, 1 AS secret FROM gacha_sr_win_comments WHERE message_1 <> message_3 ");
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
            case CODE_IMAGEURI_ID:
                return "vnd.android.cursor.item/vnd.bupjae.android.cindemasutility.card.imageuri";
            case CODE_DETAIL:
                return "vnd.android.cursor.dir/vnd.bupjae.android.cindemasutility.card.detail";
            case CODE_DETAIL_ID:
                return "vnd.android.cursor.item/vnd.bupjae.android.cindemasutility.card.detail";
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
                    Log.w(TAG, "Ignoring selection '" + selection + "' for STAT_ID");
                }
                selection = "card_id = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
                return helper.getReadableDatabase().query("stat", projection, selection, selectionArgs, null, null, sortOrder);
            }
            case CODE_IMAGEURI_ID: {
                if (!selection.equals("1")) {
                    Log.w(TAG, "Ignoring selection '" + selection + "' for IMAGEURI_ID");
                }
                MatrixCursor ret = new MatrixCursor(new String[]{"FRAMED_CARD_IMAGE", "NOFRAMED_CARD_IMAGE"}, 1);
                ret.addRow(new String[]{
                        "content://bupjae.android.cindemasutility.image/card/l/" + uri.getLastPathSegment() + ".png",
                        "content://bupjae.android.cindemasutility.image/card/xl/" + uri.getLastPathSegment() + ".webp",
                });
                return ret;
            }
            case CODE_DETAIL_ID: {
                selection = "(" + selection + ") AND card_id = ?";
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
            }
            // FALL_THROUGH
            case CODE_DETAIL: {
                return helper.getReadableDatabase().query("card_profile", projection, selection, selectionArgs, null, null, sortOrder);
            }
            case CODE_CALCULATE_ID: {
                selection = "(" + selection + ") AND card_id = ?";
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{uri.getLastPathSegment()});
                return helper.getReadableDatabase().query("calculate", projection, selection, selectionArgs, null, null, sortOrder);
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
}
