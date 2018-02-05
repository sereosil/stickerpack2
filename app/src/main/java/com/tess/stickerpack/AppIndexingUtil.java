package com.tess.stickerpack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseAppIndexingInvalidArgumentException;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.appindexing.builders.StickerBuilder;
import com.google.firebase.appindexing.builders.StickerPackBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * See firebase app indexing api code lab
 * https://codelabs.developers.google.com/codelabs/app-indexing/#0
 */

public class AppIndexingUtil {
    private static final String STICKER_FILENAME_PATTERN = "stickertesspack%s.png";
    public static Activity activity;
    private static final String CONTENT_URI_ROOT =
            String.format("content://%s/", TessStickerpackFinal.class.getName());
    private static final String STICKER_URL_PATTERN = "tessgroup://stickertesstea/%s";
    private static final String STICKER_PACK_URL_PATTERN = "tessgroup://stickertesstea/pack/%s";
    private static final String STICKER_PACK_NAME = "TessStickersFinal";
    private static final String TAG = "AppIndexingUtil";
    public static final String FAILED_TO_CLEAR_STICKERS = "Failed to clear stickers";
    public static final String FAILED_TO_INSTALL_STICKERS = "Failed to install stickers";

    public AppIndexingUtil(Activity activity) {
        this.activity = activity;
    }

    /*public static void clearStickers(final Context context, FirebaseAppIndex firebaseAppIndex) {
        Task<Void> task = firebaseAppIndex.removeAll();

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(context, "Successfully cleared stickers", Toast.LENGTH_SHORT).show();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, FAILED_TO_CLEAR_STICKERS, e);
                Toast.makeText(context, FAILED_TO_CLEAR_STICKERS, Toast.LENGTH_SHORT).show();
            }
        });
    }*/

    public static void setStickers(final Context context, FirebaseAppIndex firebaseAppIndex) {
        try {
            List<Indexable> stickers = getIndexableStickers(context);
            Indexable stickerPack = getIndexableStickerPack(context);

            List<Indexable> indexables = new ArrayList<>(stickers);
            indexables.add(stickerPack);

            Task<Void> task = firebaseAppIndex.update(
                    indexables.toArray(new Indexable[indexables.size()]));

            task.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                   // LayoutInflater inflater = null;
                   // View v = inflater.inflate(R.layout.activity_main, null, false);
                    activity.findViewById(R.id.pg_bar).setVisibility(View.GONE);
                    activity.findViewById(R.id.addStickersBtn).setVisibility(View.VISIBLE);
                    Toast.makeText(context, context.getString(R.string.stickers_add_success), Toast.LENGTH_SHORT)
                            .show();
                }
            });

            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    activity.findViewById(R.id.pg_bar).setVisibility(View.GONE);
                    activity.findViewById(R.id.addStickersBtn).setVisibility(View.VISIBLE);
                    Log.d(TAG, FAILED_TO_INSTALL_STICKERS, e);
                    Toast.makeText(context, FAILED_TO_INSTALL_STICKERS, Toast.LENGTH_SHORT)
                            .show();
                }
            });
        } catch (IOException | FirebaseAppIndexingInvalidArgumentException e) {
            Log.e(TAG, "Unable to set stickers", e);
        }
    }

    private static Indexable getIndexableStickerPack(Context context)
            throws IOException, FirebaseAppIndexingInvalidArgumentException {
        List<StickerBuilder> stickerBuilders = getStickerBuilders(context);
        File stickersDir = new File(context.getFilesDir(), "stickertesstea");

        if (!stickersDir.exists() && !stickersDir.mkdirs()) {
            throw new IOException("Stickers directory does not exist");
        }

        // Use the last sticker for category image for the sticker pack.
        final int lastIndex = stickerBuilders.size() - 1;
        final String stickerName = getStickerFilename(lastIndex);
        final String imageUrl = getStickerUrl(stickerName);

        StickerPackBuilder stickerPackBuilder = Indexables.stickerPackBuilder()
                .setName(STICKER_PACK_NAME)
                // Firebase App Indexing unique key that must match an intent-filter.
                // (e.g. mystickers://sticker/pack/0)
                .setUrl(String.format(STICKER_PACK_URL_PATTERN, lastIndex))
                // Defaults to the first sticker in "hasSticker". Used to select between sticker
                // packs so should be representative of the sticker pack.
                .setImage(imageUrl)
                .setHasSticker(stickerBuilders.toArray(new StickerBuilder[stickerBuilders.size()]))
                .setDescription("tess stickers");
        return stickerPackBuilder.build();
    }

    private static List<Indexable> getIndexableStickers(Context context) throws IOException,
            FirebaseAppIndexingInvalidArgumentException {
        List<Indexable> indexableStickers = new ArrayList<>();
        List<StickerBuilder> stickerBuilders = getStickerBuilders(context);

        for (StickerBuilder stickerBuilder : stickerBuilders) {
            stickerBuilder
                    .setIsPartOf(Indexables.stickerPackBuilder()
                            .setName(STICKER_PACK_NAME))
                    .put("keywords", "tea", "tess");
            indexableStickers.add(stickerBuilder.build());
        }

        return indexableStickers;
    }

    private static List<StickerBuilder> getStickerBuilders(Context context) throws IOException {
        List<StickerBuilder> stickerBuilders = new ArrayList<>();
        int[] stickerColors = new int[] {Color.BLACK, Color.CYAN, Color.GRAY,
                Color.WHITE, Color.MAGENTA};

        File stickersDir = new File(context.getFilesDir(), "stickertesstea");

        if (!stickersDir.exists() && !stickersDir.mkdirs()) {
            throw new IOException("Stickers directory does not exist");
        }

        for (int i = 0; i < 15; i++) {
            String stickerFilename = getStickerFilename(i);
            File stickerFile = new File(stickersDir, stickerFilename);
            String imageUrl = getStickerUrl(stickerFilename);
            writeSolidColorBitmapToFile(stickerFile, i,context);

            StickerBuilder stickerBuilder = Indexables.stickerBuilder()
                    .setName(getStickerFilename(i))
                    // Firebase App Indexing unique key that must match an intent-filter
                    // (e.g. mystickers://sticker/0)
                    .setUrl(String.format(STICKER_URL_PATTERN, i))
                    // http url or content uri that resolves to the sticker
                    // (e.g. http://www.google.com/sticker.png or content://some/path/0)
                    .setImage(imageUrl)
                    .setDescription("tess stickers")
                    .setIsPartOf(Indexables.stickerPackBuilder()
                            .setName(STICKER_PACK_NAME))
                    .put("keywords", "tea", "tess");
            stickerBuilders.add(stickerBuilder);
        }

        return stickerBuilders;
    }

    private static String getStickerFilename(int index) {
        return String.format(STICKER_FILENAME_PATTERN, index);
    }

    private static String getStickerUrl(String filename) {
        return CONTENT_URI_ROOT + filename;
    }

    /**
     * Writes a simple bitmap to local storage. The image is a solid color with size 400x400
     */
    private static void writeSolidColorBitmapToFile(File file, int i, Context context) throws IOException {
        if (!file.exists()) {
            Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
           // Drawable drawable =R.drawable.kek;
            Bitmap bm = BitmapFactory.decodeResource(context.getResources(),context.getResources().getIdentifier("st"+i, "drawable", context.getPackageName()));
          //  bitmap.eraseColor(color);
            String urlString = "http://abhiandroid-8fb4.kxcdn.com/androidstudio/wp-content/uploads/2016/02/Right-Click-on-drawable.jpg";
            URL url  = new URL(urlString);
            Bitmap bmIcon = BitmapFactory.decodeStream(url.openConnection().getInputStream());

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
               // fos.write(R.drawable.kek);
                bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        }
        else {
            /*FileOutputStream fos = null;
            try {
                file.delete();
                fos = new FileOutputStream(file);
                fos.write(R.drawable.common_full_open_on_phone);
                // bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }*/
        }
    }
}
