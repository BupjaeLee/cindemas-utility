package bupjae.android.cindemasutility;

import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

// Image type: l, lc, lls, ls, m, s, xl, xs, xs3, xs4, xxs
public class ImageProvider extends FileProvider {
    private static final String TAG = ImageProvider.class.getSimpleName();
    private static final String CARD_IMAGE_DIR = "images/card";

    private File baseDir;

    @Override
    public boolean onCreate() {
        baseDir = new File(Environment.getExternalStorageDirectory(), "kr.daum_mobage.am_db.g13001173/imas_cg_assets_android");
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        try {
            return super.openFile(uri, mode);
        } catch (FileNotFoundException ex) {
            List<String> segments = uri.getPathSegments();
            if (segments == null || segments.isEmpty()) throw ex;
            switch (segments.get(0)) {
                case "card":
                    if (segments.size() != 3) {
                        throw new IllegalArgumentException("Wrong URI for card: " + uri);
                    }
                    prepareCardImage(segments.get(1), segments.get(2));
                    break;
                default:
                    throw ex;
            }
        }
        return super.openFile(uri, mode);
    }

    private void prepareCardImage(String type, String name) throws FileNotFoundException {
        File dir = new File(getContext().getCacheDir(), "images/card/" + type);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        File original = new File(new File(new File(baseDir, CARD_IMAGE_DIR), type), name);
        byte[] buffer = readFully(original);

        byte mask;
        if (name.endsWith(".png")) {
            mask = (byte) (buffer[0] ^ 137);
        } else if (name.endsWith(".webp")) {
            mask = (byte) (buffer[0] ^ 'R');
        } else {
            mask = 0;
        }
        for (int i = 0; i < 50; i++) buffer[i] ^= mask;

        File cacheFile = new File(dir, name);
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
        }
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
