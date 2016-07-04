package jp.torifuku.actionimagecapturesample.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;

import jp.torifuku.actionimagecapturesample.R;
import jp.torifuku.actionimagecapturesample.asynctasks.ImageDecodeTask;

/**
 * CameraPreviewFragment
 */
public class CameraPreviewFragment extends Fragment {

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 100;

    private static final String FILE_PATH = "file_path";

    private ImageView imageView;

    private File file;

    private ImageDecodeTask task;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_preview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button captureButton = (Button) view.findViewById(R.id.capture_button);
        captureButton.setOnClickListener(v -> capture());

        imageView = (ImageView) view.findViewById(R.id.imageView);

        if (savedInstanceState != null) {
            String filePath = savedInstanceState.getString(FILE_PATH, null);
            if (!TextUtils.isEmpty(filePath)) {
                file = new File(filePath);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showImageAsync();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (task != null && !task.isCancelled()) {
            task.cancel(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (file != null) {
            outState.putString(FILE_PATH, file.getAbsolutePath());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                galleryAddPic();
            } else {
                file = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    capture();
                }
        }
    }

    private void capture() {
        if (!isWriteExternalStoragePermissionGranted()) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                View view = getView();
                if (view == null) {
                    return;
                }
                Snackbar snackbar = Snackbar.make(view, "ストレージへのアクセスを許可してください", Snackbar.LENGTH_LONG);
                snackbar.setAction("許可する", v -> requestWriteExternalStoragePermission());
                snackbar.show();
            } else {
                requestWriteExternalStoragePermission();
            }
            return;
        }

        // Refer  https://developer.android.com/guide/topics/media/camera.html
        // Refer https://developer.android.com/training/camera/photobasics.html
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = outputMediaFile();
        if (file == null) {
            return;
        }
        Uri photoURI = FileProvider.getUriForFile(getContext(),
                "jp.torifuku.actionimagecapturesample",
                file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    private boolean isWriteExternalStoragePermissionGranted() {
        Activity activity = getActivity();
        return activity != null && ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestWriteExternalStoragePermission() {
        requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    private File outputMediaFile() {
        File dir = dir();
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }

        return new File(dir, "" + System.currentTimeMillis() + ".jpg");
    }

    private File dir() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getContext().getPackageName());
    }

    // https://developer.android.com/training/camera/photobasics.html
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        getContext().sendBroadcast(mediaScanIntent);
    }

    private void showImageAsync() {
        if (file == null || !file.exists()) {
            return;
        }
        task = new ImageDecodeTask(this::setImageBitmap);
        task.execute(file);
    }

    private void setImageBitmap(Bitmap bitmap) {
        if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
    }
}
