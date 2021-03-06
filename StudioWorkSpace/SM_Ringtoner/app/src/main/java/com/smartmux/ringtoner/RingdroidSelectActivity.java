
package com.smartmux.ringtoner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.smartmux.audiofile.CheapSoundFile;

import java.io.File;
import java.util.ArrayList;

/**
 * Main screen that shows up when you launch SM_Ringtoner.  Handles selecting
 * an audio file or using an intent to record a new one, and then
 * launches RingdroidEditActivity from here.
 */
public class RingdroidSelectActivity
    extends ListActivity
{
    private SimpleCursorAdapter mAdapter;
    private boolean mWasGetContentIntent;
    private boolean mShowAll;

    // Result codes
    private static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_CODE_CHOOSE_CONTACT = 2;

    // Context menu
    private static final int CMD_EDIT = 4;
    private static final int CMD_DELETE = 5;
    private static final int CMD_SET_AS_DEFAULT = 6;
    private static final int CMD_SET_AS_CONTACT = 7;


    public RingdroidSelectActivity() {
    }

//    public void setPermission(){
//
//        PermissionListener permissionlistener = new PermissionListener() {
//            @Override
//            public void onPermissionGranted() {
//                // Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
//                // Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
//            }
//
//
//        };
//
//
//        new TedPermission(this)
//                .setPermissionListener(permissionlistener)
//                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
//                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.WRITE_SETTINGS)
//                .check();
//    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mShowAll = false;
       
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            showFinalAlert(getResources().getText(R.string.sdcard_readonly));
            return;
        }
        if (status.equals(Environment.MEDIA_SHARED)) {
            showFinalAlert(getResources().getText(R.string.sdcard_shared));
            return;
        }
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            showFinalAlert(getResources().getText(R.string.no_sdcard));
            return;
        }

        
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			mWasGetContentIntent = intent != null;

		// Inflate our UI from its XML layout description.
        setContentView(R.layout.media_select);

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                // Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();

                try {
                    mAdapter = new SimpleCursorAdapter(
                            RingdroidSelectActivity.this,
                            // Use a template that displays a text view
                            R.layout.media_select_row,
                            // Give the cursor to the list adatper
                            createCursor(""),
                            // Map from database columns...
                            new String[] {
                                    MediaStore.Audio.Media.ARTIST,
                                    MediaStore.Audio.Media.ALBUM,
                                    MediaStore.Audio.Media.TITLE,
                                    MediaStore.Audio.Media._ID,
                                    MediaStore.Audio.Media._ID},
                            // To widget ids in the row layout...
                            new int[] {
                                    R.id.row_artist,
                                    R.id.row_album,
                                    R.id.row_title,
                                    R.id.row_icon,
                                    R.id.row_options_button});

                    setListAdapter(mAdapter);

                    getListView().setItemsCanFocus(true);

                    // Normal click - open the editor
                    getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView parent,
                                                View view,
                                                int position,
                                                long id) {
                            startRingdroidEditor();
                        }
                    });

                } catch (SecurityException e) {
                    // No permission to retrieve audio?
                    Log.e("SM_Ringtoner", e.toString());

                    // todo error 1
                } catch (IllegalArgumentException e) {
                    // No permission to retrieve audio?
                    Log.e("SM_Ringtoner", e.toString());

                    // todo error 2
                }

                mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                    public boolean setViewValue(View view,
                                                Cursor cursor,
                                                int columnIndex) {
                        if (view.getId() == R.id.row_options_button){
                            // Get the arrow image view and set the onClickListener to open the context menu.
                            ImageView iv = (ImageView)view;
                            iv.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    openContextMenu(v);
                                }
                            });
                            return true;
                        } else if (view.getId() == R.id.row_icon) {
                            setSoundIconFromCursor((ImageView) view, cursor);
                            return true;
                        }

                        return false;
                    }
                });

                // Long-press opens a context menu
                registerForContextMenu(getListView());
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                // Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }


        };


        new TedPermission(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,Manifest.permission.RECORD_AUDIO)
                .check();


        
    }

    private void setSoundIconFromCursor(ImageView view, Cursor cursor) {
        if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_RINGTONE))) {
            view.setImageResource(R.drawable.type_ringtone);
//            ((View) view.getParent()).setBackgroundColor(
//                    getResources().getColor(R.drawable.type_bkgnd_ringtone));

            ((View) view.getParent()).setBackgroundColor(Color.parseColor("#88008888"));

        } else if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_ALARM))) {
            view.setImageResource(R.drawable.type_alarm);
//            ((View) view.getParent()).setBackgroundColor(
//                    getResources().getColor(R.drawable.type_bkgnd_alarm));

            ((View) view.getParent()).setBackgroundColor(Color.parseColor("#88880000"));

        } else if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_NOTIFICATION))) {
            view.setImageResource(R.drawable.type_notification);
//            ((View) view.getParent()).setBackgroundColor(
//                    getResources().getColor(R.drawable.type_bkgnd_notification));

            ((View) view.getParent()).setBackgroundColor(Color.parseColor("#88666600"));

        } else if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_MUSIC))) {
            view.setImageResource(R.drawable.type_music);
//            ((View) view.getParent()).setBackgroundColor(
//                    getResources().getColor(R.drawable.type_bkgnd_music));

            ((View) view.getParent()).setBackgroundColor(Color.parseColor("#88000000"));

        }

        String filename = cursor.getString(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.DATA));
        if (!CheapSoundFile.isFilenameSupported(filename)) {
//            ((View) view.getParent()).setBackgroundColor(
//                    getResources().getColor(R.drawable.type_bkgnd_unsupported));

            ((View) view.getParent()).setBackgroundColor(Color.parseColor("#88cccccc"));
        }
    }

    /** Called with an Activity we started with an Intent returns. */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent dataIntent) {
        if (requestCode != REQUEST_CODE_EDIT) {
            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        setResult(RESULT_OK, dataIntent);
        finish();
    }

    @SuppressLint("NewApi")
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.select_options, menu);
	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_record:
            onRecord();
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,
            View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        Cursor c = mAdapter.getCursor();
        String title = c.getString(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.TITLE));

        menu.setHeaderTitle(title);

        menu.add(0, CMD_EDIT, 0, R.string.context_menu_edit);
        menu.add(0, CMD_DELETE, 0, R.string.context_menu_delete);

        // Add items to the context menu item based on file type
        if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_RINGTONE))) {
            menu.add(0, CMD_SET_AS_DEFAULT, 0, R.string.context_menu_default_ringtone);
            menu.add(0, CMD_SET_AS_CONTACT, 0, R.string.context_menu_contact);
        } else if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_NOTIFICATION))) {
            menu.add(0, CMD_SET_AS_DEFAULT, 0, R.string.context_menu_default_notification);
	}
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info =
            (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case CMD_EDIT:
            startRingdroidEditor();
            return true;
        case CMD_DELETE:
            confirmDelete();
            return true;
        case CMD_SET_AS_DEFAULT:
            setAsDefaultRingtoneOrNotification();
            return true;
        case CMD_SET_AS_CONTACT:
            return chooseContactForRingtone(item);
        default:
            return super.onContextItemSelected(item);
        }
    }

    private void setAsDefaultRingtoneOrNotification(){
        Cursor c = mAdapter.getCursor();

        // If the item is a ringtone then set the default ringtone, 
        // otherwise it has to be a notification so set the default notification sound
        if (0 != c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE))){
            RingtoneManager.setActualDefaultRingtoneUri(
                    RingdroidSelectActivity.this,
                    RingtoneManager.TYPE_RINGTONE,
                    getUri());
            Toast.makeText(
                    RingdroidSelectActivity.this,
                    R.string.default_ringtone_success_message,
                    Toast.LENGTH_SHORT)
                    .show();
        } else {
            RingtoneManager.setActualDefaultRingtoneUri(
                    RingdroidSelectActivity.this,
                    RingtoneManager.TYPE_NOTIFICATION,
                    getUri());
            Toast.makeText(
                    RingdroidSelectActivity.this,
                    R.string.default_notification_success_message,
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private Uri getUri(){
        //Get the uri of the item that is in the row
        Cursor c = mAdapter.getCursor();
        int uriIndex = c.getColumnIndex(
                "\"" + MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\"");
        if (uriIndex == -1) {
            uriIndex = c.getColumnIndex(
                    "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\"");
        }
        String itemUri = c.getString(uriIndex) + "/" +
        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
        return (Uri.parse(itemUri));
    }

    private boolean chooseContactForRingtone(MenuItem item){
        try {
            //Go to the choose contact activity
            Intent intent = new Intent(Intent.ACTION_EDIT, getUri());
            intent.setClassName(
                    "com.smartmux.ringtoner",
            "com.smartmux.ringtoner.ChooseContactActivity");
            startActivityForResult(intent, REQUEST_CODE_CHOOSE_CONTACT);
        } catch (Exception e) {
            Log.e("SM_Ringtoner", "Couldn't open Choose Contact window");
        }
        return true;
    }

    private void confirmDelete() {
        // See if the selected list item was created by Ringdroid to
        // determine which alert message to show
        Cursor c = mAdapter.getCursor();
        int artistIndex = c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ARTIST);
        String artist = c.getString(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ARTIST));
        CharSequence ringdroidArtist =
            getResources().getText(R.string.artist_name);

        CharSequence message;
        if (artist.equals(ringdroidArtist)) {
            message = getResources().getText(
                    R.string.confirm_delete_ringdroid);
        } else {
            message = getResources().getText(
                    R.string.confirm_delete_non_ringdroid);
        }

        CharSequence title;
        if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_RINGTONE))) {
            title = getResources().getText(R.string.delete_ringtone);
        } else if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_ALARM))) {
            title = getResources().getText(R.string.delete_alarm);
        } else if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_NOTIFICATION))) {
            title = getResources().getText(R.string.delete_notification);
        } else if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_MUSIC))) {
            title = getResources().getText(R.string.delete_music);
        } else {
            title = getResources().getText(R.string.delete_audio);
        }

        new AlertDialog.Builder(RingdroidSelectActivity.this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                R.string.delete_ok_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                        onDelete();
                    }
                })
            .setNegativeButton(
                R.string.delete_cancel_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                    }
                })
            .setCancelable(true)
            .show();
    }

    private void onDelete() {
        Cursor c = mAdapter.getCursor();
        int dataIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        String filename = c.getString(dataIndex);

        int uriIndex = c.getColumnIndex(
                "\"" + MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\"");
        if (uriIndex == -1) {
            uriIndex = c.getColumnIndex(
                    "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\"");
        }
        if (uriIndex == -1) {
            showFinalAlert(getResources().getText(R.string.delete_failed));
            return;
        }

        if (!new File(filename).delete()) {
            showFinalAlert(getResources().getText(R.string.delete_failed));
        }

        String itemUri = c.getString(uriIndex) + "/" +
        c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
        getContentResolver().delete(Uri.parse(itemUri), null, null);
    }

    private void showFinalAlert(CharSequence message) {
        new AlertDialog.Builder(RingdroidSelectActivity.this)
        .setTitle(getResources().getText(R.string.alert_title_failure))
        .setMessage(message)
        .setPositiveButton(
                R.string.alert_ok_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void onRecord() {
        try {
            Intent intent = new Intent(Intent.ACTION_EDIT,
                    Uri.parse("record"));
            intent.putExtra("was_get_content_intent",
                    mWasGetContentIntent);
            intent.setClassName(
                    "com.smartmux.ringtoner",
            "com.smartmux.ringtoner.RingdroidEditActivity");
            startActivityForResult(intent, REQUEST_CODE_EDIT);
        } catch (Exception e) {
            Log.e("SM_Ringtoner", "Couldn't start editor");
        }
    }

    private void startRingdroidEditor() {
        Cursor c = mAdapter.getCursor();
        int dataIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        String filename = c.getString(dataIndex);
        try {
            Intent intent = new Intent(Intent.ACTION_EDIT,
                    Uri.parse(filename));
            intent.putExtra("was_get_content_intent",
                    mWasGetContentIntent);
            intent.setClassName(
                    "com.smartmux.ringtoner",
            "com.smartmux.ringtoner.RingdroidEditActivity");
            startActivityForResult(intent, REQUEST_CODE_EDIT);
        } catch (Exception e) {
            Log.e("SM_Ringtoner", "Couldn't start editor");
        }
    }

    private Cursor getInternalAudioCursor(String selection,
            String[] selectionArgs) {
        return managedQuery(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                INTERNAL_COLUMNS,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    private Cursor getExternalAudioCursor(String selection,
            String[] selectionArgs) {
        return managedQuery(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                EXTERNAL_COLUMNS,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    Cursor createCursor(String filter) {
        ArrayList<String> args = new ArrayList<String>();
        String selection;

        if (mShowAll) {
            selection = "(_DATA LIKE ?)";
            args.add("%");
        } else {
            selection = "(";
            for (String extension : CheapSoundFile.getSupportedExtensions()) {
                args.add("%." + extension);
                if (selection.length() > 1) {
                    selection += " OR ";
                }
                selection += "(_DATA LIKE ?)";
            }
            selection += ")";

            selection = "(" + selection + ") AND (_DATA NOT LIKE ?)";
            args.add("%espeak-data/scratch%");
        }

        if (filter != null && filter.length() > 0) {
            filter = "%" + filter + "%";
            selection =
                "(" + selection + " AND " +
                "((TITLE LIKE ?) OR (ARTIST LIKE ?) OR (ALBUM LIKE ?)))";
            args.add(filter);
            args.add(filter);
            args.add(filter);
        }

        String[] argsArray = args.toArray(new String[args.size()]);

        Cursor external = getExternalAudioCursor(selection, argsArray);
        Cursor internal = getInternalAudioCursor(selection, argsArray);

        Cursor c = new MergeCursor(new Cursor[] {
                getExternalAudioCursor(selection, argsArray),
                getInternalAudioCursor(selection, argsArray)});
        startManagingCursor(c);
        return c;
    }

    private static final String[] INTERNAL_COLUMNS = new String[] {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.IS_RINGTONE,
        MediaStore.Audio.Media.IS_ALARM,
        MediaStore.Audio.Media.IS_NOTIFICATION,
        MediaStore.Audio.Media.IS_MUSIC,
        "\"" + MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\""
    };

    private static final String[] EXTERNAL_COLUMNS = new String[] {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.IS_RINGTONE,
        MediaStore.Audio.Media.IS_ALARM,
        MediaStore.Audio.Media.IS_NOTIFICATION,
        MediaStore.Audio.Media.IS_MUSIC,
        "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\""
    };

}
