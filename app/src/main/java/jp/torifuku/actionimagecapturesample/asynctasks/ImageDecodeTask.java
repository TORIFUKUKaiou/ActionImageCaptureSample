package jp.torifuku.actionimagecapturesample.asynctasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * ImageDecodeTask
 */
public class ImageDecodeTask extends AsyncTask<File, Void, Bitmap> {

    public interface OnCompleteListener {
        void onComplete(Bitmap bitmap);
    }

    private final OnCompleteListener listener;

    public ImageDecodeTask(OnCompleteListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    protected Bitmap doInBackground(File... files) {
        if (isCancelled()) {
            return null;
        }
        return decodeFile(files[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled() || bitmap == null) {
            return;
        }

        if (listener != null) {
            listener.onComplete(bitmap);
        }
    }

    private Bitmap decodeFile(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inSampleSize = 2;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
}
