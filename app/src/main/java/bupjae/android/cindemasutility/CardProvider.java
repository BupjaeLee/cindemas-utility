package bupjae.android.cindemasutility;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
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
    private static final String CARD_IMAGE_DIR = "images/card";

    private static final int CODE_BASE = 1;
    private static final int CODE_BASE_ID = 2;
    private static final int CODE_IMAGE = 3;
    private static final int CODE_EVOLVE = 4;
    private static final int CODE_EVOLVE_ID = 5;
    private static final int CODE_COMMENTS = 6;
    private static final int CODE_COMMENTS_ID = 7;
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
            @Override
            public void onCreate(SQLiteDatabase db) {
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            }

            @Override
            public void onOpen(SQLiteDatabase db) {
                super.onOpen(db);
                db.execSQL("ATTACH DATABASE ? AS card_data", new Object[]{new File(baseDir, CARD_DB_FILENAME).toString()});
                db.execSQL("ATTACH DATABASE ? AS composition", new Object[]{new File(baseDir, CARD_COMPOSITION_FILENAME).toString()});
                db.execSQL("ATTACH DATABASE ? AS card_comments", new Object[]{new File(baseDir, CARD_COMMENTS_FILENAME).toString()});
                db.execSQL("CREATE TEMP VIEW base AS " +
                        "SELECT *, card_id AS _id FROM card_data");
                db.execSQL("CREATE TEMP VIEW evolve AS " +
                        "SELECT " +
                        "c1.card_id AS _id, " +
                        "c1.card_id AS card_id, " +
                        "c1.evo_card_id AS evo_after_id, " +
                        "c3.card_name AS evo_after_name, " +
                        "IFNULL(c2.card_id, 0) AS evo_before_id, " +
                        "c2.card_name AS evo_before_name " +
                        "FROM composition AS c1 " +
                        "LEFT JOIN composition AS c2 ON c1.card_id = c2.evo_card_id " +
                        "LEFT JOIN composition AS c3 ON c1.evo_card_id = c3.card_id");
                db.execSQL("CREATE TEMP VIEW comments AS " +
                        "SELECT card_id*100+1 AS _id, card_id, 'my_1' AS comments_key, comments_my_1 AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+2 AS _id, card_id, 'my_2' AS comments_key, comments_my_2 AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+3 AS _id, card_id, 'my_3' AS comments_key, comments_my_3 AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+4 AS _id, card_id, 'my_4' AS comments_key, comments_my_4 AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+5 AS _id, card_id, 'my_max' AS comments_key, comments_my_max AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+6 AS _id, card_id, 'work_1' AS comments_key, comments_work_1 AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+7 AS _id, card_id, 'work_2' AS comments_key, comments_work_2 AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+8 AS _id, card_id, 'work_3' AS comments_key, comments_work_3 AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+9 AS _id, card_id, 'work_4' AS comments_key, comments_work_4 AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+10 AS _id, card_id, 'work_max' AS comments_key, comments_work_max AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+11 AS _id, card_id, 'live' AS comments_key, comments_live AS comments_value FROM card_comments " +
                        "UNION SELECT card_id*100+12 AS _id, card_id, 'love_max' AS comments_key, comments_love_max AS comments_value FROM card_comments");
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
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        switch (uriMatcher.match(uri)) {
            case CODE_BASE_ID: {
                if (selection != null && selection.trim().length() != 0) {
                    Log.w(TAG, "Ignoring selection '" + selection + "' for single row query");
                }
                selection = "card_id = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
            }
            // FALL_THROUGH
            case CODE_BASE: {
                return helper.getReadableDatabase().query("base", projection, selection, selectionArgs, null, null, sortOrder);
            }
            case CODE_IMAGE: {
                if (selection != null && selection.trim().length() != 0) {
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
                if (selection != null && selection.trim().length() != 0) {
                    Log.w(TAG, "Ignoring selection '" + selection + "' for single row query");
                }
                selection = "card_id = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
            }
            // FALL_THROUGH
            case CODE_EVOLVE: {
                return helper.getReadableDatabase().query("evolve", projection, selection, selectionArgs, null, null, sortOrder);
            }
            case CODE_COMMENTS_ID: {
                if (selection != null && selection.trim().length() != 0) {
                    Log.w(TAG, "Ignoring selection '" + selection + "' for single row query");
                }
                selection = "card_id = ?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
            }
            // FALL_THROUGH
            case CODE_COMMENTS: {
                return helper.getReadableDatabase().query("comments", projection, selection, selectionArgs, null, null, sortOrder);
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
                    File tmpFile = File.createTempFile("card_image", getCardImageExtension(uri));
                    OutputStream os = new FileOutputStream(tmpFile);
                    try {
                        os.write(buffer);
                    } finally {
                        try {
                            os.close();
                        } catch (IOException ex) {
                            Log.w(TAG, "openFile(): closing failed", ex);
                        }
                    }
                    return ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_ONLY);
                } catch (IOException ex) {
                    Log.e(TAG, "openFile(): writing failed", ex);
                    return null;
                }
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
}
