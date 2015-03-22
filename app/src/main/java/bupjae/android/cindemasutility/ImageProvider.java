package bupjae.android.cindemasutility;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
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

// Image type: l, lc, lls, ls, m, s, xl, xs, xs3, xs4, xxs
public class ImageProvider extends ContentProvider {
    public static final String AUTHORITY = "bupjae.android.cindemasutility.image";

    private static final String TAG = ImageProvider.class.getSimpleName();

    private File baseDir;

    @Override
    public boolean onCreate() {
        baseDir = new File(Environment.getExternalStorageDirectory(), "kr.daum_mobage.am_db.g13001173/imas_cg_assets_android/images");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (projection == null)
            projection = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor ret = new MatrixCursor(projection, 1);
        MatrixCursor.RowBuilder builder = ret.newRow();
        for (String column : projection) {
            switch (column) {
                case OpenableColumns.DISPLAY_NAME:
                    builder.add(uri.getLastPathSegment());
                    break;
                case OpenableColumns.SIZE:
                    builder.add(getOriginalFile(uri).length());
                    break;
                case "_data":
                    builder.add(getConvertedFile(uri).toString());
                    break;
                default:
                    builder.add(null);
            }
        }
        return ret;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!mode.equals("r")) {
            throw new FileNotFoundException("Unsupported mode: " + mode);
        }
        File original = getOriginalFile(uri);
        if (!original.exists()) {
            throw new FileNotFoundException("Not found: " + original);
        }
        File converted = getConvertedFile(uri);
        if (!converted.exists() || converted.lastModified() < original.lastModified() || converted.length() != original.length()) {
            try {
                prepareFile(original, converted, getType(uri));
            } catch (IOException ex) {
                Log.e(TAG, "openFile: prepareFile failed", ex);
            }
        }
        return ParcelFileDescriptor.open(converted, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        if (uri.getLastPathSegment().endsWith(".webp")) {
            return "image/webp";
        }
        if (uri.getLastPathSegment().endsWith(".png")) {
            return "image/png";
        }
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.w(TAG, "insert(): this content provider is read-only");
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.w(TAG, "delete(): this content provider is read-only");
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.w(TAG, "update(): this content provider is read-only");
        return 0;
    }

    private File getOriginalFile(Uri uri) {
        return new File(baseDir, uri.getPath());
    }

    private File getConvertedFile(Uri uri) {
        return new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), uri.getPath());
    }

    private static void prepareFile(File original, File converted, String type) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        converted.getParentFile().mkdirs();
        byte[] data = readFully(original);
        byte mask;
        switch (type) {
            case "image/png":
                mask = (byte) (data[0] ^ 137);
                break;
            case "image/webp":
                mask = (byte) (data[0] ^ 'R');
                break;
            default:
                mask = 0;
        }
        for (int i = 0; i < 50; i++) data[i] ^= mask;
        FileOutputStream fout = new FileOutputStream(converted);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            fout.write(data);
        } finally {
            fout.close();
        }
    }

    private static byte[] readFully(File file) throws IOException {
        InputStream fin = new FileInputStream(file);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            while (true) {
                int n = fin.read(buf);
                if (n == -1) break;
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Log.e(TAG, "prepareFile: failed closing original file", ex);
            }
        }
    }
}
