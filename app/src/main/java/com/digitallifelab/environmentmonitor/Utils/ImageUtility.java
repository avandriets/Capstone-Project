package com.digitallifelab.environmentmonitor.Utils;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.digitallifelab.environmentmonitor.Data.PicturesStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtility {

    private static final String LOG_TAG = ImageUtility.class.getSimpleName();

    public static void RefreshGallery(String fileName, Context context){

        MediaScannerConnection.scanFile(context,
                new String[]{fileName},
                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    public static Uri addImageToGallery(final String filePath, final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static void DeleteImageFromGalleryBase(final Context context, String fileName) {

        // Set up the projection (we only need the ID)
        String[] projection = {MediaStore.Images.Media._ID};

        // Match on the file path
        String selection = MediaStore.Images.Media.DATA + " = ?";
        String[] selectionArgs = new String[]{fileName};

        // Query for the ID of the media matching the file path
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
        if (c.moveToFirst()) {
            // We found the ID. Deleting the item via the content provider will also remove the file
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            contentResolver.delete(deleteUri, null, null);
        } else {
            // File not found in media store DB
        }
        c.close();
    }

    public static String getRealPathFromURI(Uri contentUri, ContentResolver content_resolver) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = content_resolver.query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static String getThumbnailPathFromURI(Uri contentUri, ContentResolver content_resolver) {

        String uri = "";

        Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
                content_resolver, ContentUris.parseId(contentUri),
                MediaStore.Images.Thumbnails.MINI_KIND,
                null);

        if( cursor != null && cursor.getCount() > 0 ) {
            cursor.moveToFirst();//**EDIT**
            uri = cursor.getString( cursor.getColumnIndex( MediaStore.Images.Thumbnails.DATA ) );
        }

        return uri;
    }


    public static void AddImageToGallery(final Context context, String mImageFileName) {

        try {

            String newCreatedFile = MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    mImageFileName,
                    Utility.IMAGE_FILE_NAME + System.currentTimeMillis(),
                    "");

            String newFilePath = Uri.fromFile(new File(getRealPathFromURI(Uri.parse(newCreatedFile), context.getContentResolver()))).toString();
            //mArrayOfOriginalImages.add(newFilePath);

            String thumbNailFilePath = getThumbnailPathFromURI(Uri.parse(newCreatedFile), context.getContentResolver());

            thumbNailFilePath = Uri.fromFile(new File(thumbNailFilePath)).toString();
            //mArrayOfImageThumbNails.add(Uri.parse(thumbNailFilePath).toString());

            File file = new File(mImageFileName);
            file.delete();


            Log.d(LOG_TAG, "Image created");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static File ReduceImageFileSize(String originalFileName, Activity activity){

        ExifInterface exif = null;
        String orientString;
        int orientation;
        int rotationAngle = 0;
        Matrix matrix = new Matrix();

        File                newFullFile = null;
        FileOutputStream    out;

        Bitmap bmp = BitmapFactory.decodeFile(originalFileName);

        try {
            exif = new ExifInterface(originalFileName);
            orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

            orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

            matrix.setRotate(rotationAngle, (float) bmp.getWidth()/2, (float) bmp.getHeight()/2);

            //Уменьшаем в половину по пикселям
            bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 2, bmp.getHeight() / 2, false);

            //Переворачиваем
            if(rotationAngle != 0) {
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }

            newFullFile = Utility.createImageFile(activity, Utility.IMAGE_FILE_NAME + "_" + System.currentTimeMillis());
            out = new FileOutputStream(newFullFile);

            bmp.compress(Bitmap.CompressFormat.JPEG, 50, out);
            out.flush();
            out.close();

        } catch (IOException | NullPointerException e) {
            Log.e(LOG_TAG, "Took exception " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(activity, "Cannot read an image", Toast.LENGTH_SHORT).show();
        }

        return newFullFile;
    }

}
