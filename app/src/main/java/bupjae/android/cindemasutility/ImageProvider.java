package bupjae.android.cindemasutility;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// Image type: l, lc, lls, ls, m, s, xl, xs, xs3, xs4, xxs
public class ImageProvider extends ContentProvider {
    public static final String AUTHORITY = "bupjae.android.cindemasutility.image";
    public static final String EXTRA_EXPORTED_URI = "bupjae.android.cindemasutility.image.exported_uri";
    public static final String EXTRA_EXPORTED_TYPE = "bupjae.android.cindemasutility.image.exported_type";

    private static final String TAG = ImageProvider.class.getSimpleName();

    private File baseDir;

    @Override
    public boolean onCreate() {
        baseDir = new File(Environment.getExternalStorageDirectory(), "kr.daum_mobage.am_db.g13001173/imas_cg_assets_android/images");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // TODO: implement cursor with OpenableColumns
        return null;
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

        return openPipeHelper(uri, getType(uri), null, new FileInputStream(original), new PipeDataWriter<InputStream>() {
            @Override
            public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, final String mimeType, Bundle opts, InputStream input) {
                InputStream fin = new FilterInputStream(input) {
                    private int cnt = 0;
                    private byte mask;

                    @Override
                    public int read() throws IOException {
                        byte[] buffer = new byte[1];
                        return read(buffer) == -1 ? -1 : (buffer[0] & 0xff);
                    }

                    @Override
                    public int read(@NonNull byte[] buffer) throws IOException {
                        return read(buffer, 0, buffer.length);
                    }

                    @Override
                    public int read(@NonNull byte[] buffer, int byteOffset, int byteCount) throws IOException {
                        int ret = super.read(buffer, byteOffset, byteCount);
                        if (ret <= 0) return ret;
                        if (cnt == 0) {
                            switch (mimeType) {
                                case "image/png":
                                    mask = (byte) (buffer[byteOffset] ^ 137);
                                    break;
                                case "image/webp":
                                    mask = (byte) (buffer[byteOffset] ^ 'R');
                                    break;
                            }
                        }
                        for (int i = byteOffset; i < byteOffset + ret && cnt < 50; i++, cnt++) {
                            buffer[i] ^= mask;
                        }
                        return ret;
                    }
                };
                OutputStream fout = new ParcelFileDescriptor.AutoCloseOutputStream(output);
                byte[] buf = new byte[1024];
                try {
                    while (true) {
                        int n = fin.read(buf);
                        if (n == -1) break;
                        fout.write(buf, 0, n);
                        fout.flush();
                    }
                } catch (IOException ex) {
                    // EPIPE likely means pipe closed on other end; treat it as WAI.
                    if (!ex.getMessage().contains("EPIPE")) {
                        Log.w(TAG, "openFile failed", ex);
                    }
                } finally {
                    try {
                        fin.close();
                    } catch (IOException ex) {
                        Log.w(TAG, "openFile failed closing input", ex);
                    }
                    try {
                        fout.close();
                    } catch (IOException ex) {
                        Log.w(TAG, "openFile failed closing output", ex);
                    }
                }
            }
        });
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

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        switch (method) {
            case "export":
                return callExport(arg);
            default:
                return super.call(method, arg, extras);
        }
    }

    private Bundle callExport(String arg) {
        Uri source = Uri.parse(arg);
        if (!source.getScheme().equals("content") || !source.getAuthority().equals(AUTHORITY)) {
            Log.e(TAG, "callExport(): unsupported source: " + source);
            return null;
        }
        File target = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), source.getPath());
        //noinspection ResultOfMethodCallIgnored
        target.getParentFile().mkdirs();

        if (!target.exists()) {
            try {
                InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(openFile(source, "r"));
                OutputStream os = new FileOutputStream(target);
                byte[] buf = new byte[1024];
                try {
                    while (true) {
                        int n = is.read(buf);
                        if (n == -1) break;
                        os.write(buf, 0, n);
                        os.flush();
                    }
                } finally {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        Log.e(TAG, "callExport(): error closing input", ex);
                    }
                    try {
                        os.close();
                    } catch (IOException ex) {
                        Log.e(TAG, "callExport(): error closing output", ex);
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "callExport(): File I/O error: " + target);
                return null;
            }
        }

        Bundle ret = new Bundle();
        ret.putParcelable(EXTRA_EXPORTED_URI, Uri.fromFile(target));
        ret.putString(EXTRA_EXPORTED_TYPE, getType(source));
        return ret;
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
}
