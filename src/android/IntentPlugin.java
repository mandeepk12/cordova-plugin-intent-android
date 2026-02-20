package com.betasoft.cordova.plugin.intent;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;


public class IntentPlugin extends CordovaPlugin {

    private final String pluginName = "IntentPlugin";
    private CallbackContext onNewIntentCallbackContext = null;

    private Intent bootIntent = null;

    @Override
    protected void pluginInitialize() {
        Log.d(pluginName,   "Initialized" );
        final Intent cordovaIntent = this.cordova.getActivity().getIntent();
        final String intentAction = cordovaIntent.getAction();
        if(intentAction == Intent.ACTION_SEND || intentAction == Intent.ACTION_SEND_MULTIPLE){
            this.bootIntent = cordovaIntent;
        }
    }

    protected void  handleBootIntent(final CallbackContext callbackContext){
        try {
            final JSONObject data = getIntentJson(this.bootIntent);
            assert data != null;
            PluginResult result = new PluginResult(PluginResult.Status.OK, data);
            result.setKeepCallback(true);
            this.bootIntent = null;
            callbackContext.sendPluginResult(result);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Generic plugin command executor
     *
     * @param action
     * @param data
     * @param callbackContext
     * @return
     */
    @Override
    public boolean execute(final String action, final JSONArray data, final CallbackContext callbackContext) {
        Log.d(pluginName, pluginName + " called with options: " + data);

        Class params[] = new Class[2];
        params[0] = JSONArray.class;
        params[1] = CallbackContext.class;

        try {
            Method method = this.getClass().getDeclaredMethod(action, params);
            method.invoke(this, data, callbackContext);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }

    /**
     * Send a JSON representation of the cordova intent back to the caller
     *
     * @param data
     * @param context
     */
    public boolean getCordovaIntent (final JSONArray data, final CallbackContext context) {
        if(data.length() != 0) {
            context.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        }

        Intent intent = cordova.getActivity().getIntent();
        context.sendPluginResult(new PluginResult(PluginResult.Status.OK, getIntentJson(intent)));
        return true;
    }

    /**
     * Register handler for onNewIntent event
     *
     * @param data
     * @param context
     * @return
     */
    public boolean setNewIntentHandler (final JSONArray data, final CallbackContext context) {
        if(data.length() != 1) {
            context.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        }

        this.onNewIntentCallbackContext = context;

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        context.sendPluginResult(result);
        if(this.bootIntent != null){
            this.handleBootIntent(context);
        }
        return true;
    }

    /**
     * Triggered on new intent
     *
     * @param intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        if (this.onNewIntentCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
            result.setKeepCallback(true);
            this.onNewIntentCallbackContext.sendPluginResult(result);
        }
    }

    /**
     * Return JSON representation of intent attributes
     *
     * @param intent
     * @return
     */
    private JSONObject getIntentJson(Intent intent) {
        JSONObject intentJSON = null;
        ClipData clipData = null;
        JSONObject[] items = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            clipData = intent.getClipData();
            if (clipData != null) {
                int clipItemCount = clipData.getItemCount();
                items = new JSONObject[clipItemCount];

                for (int i = 0; i < clipItemCount; i++) {
                    ClipData.Item item = clipData.getItemAt(i);

                    try {
                        items[i] = new JSONObject();
                        items[i].put("htmlText", item.getHtmlText());
                        items[i].put("intent", item.getIntent());
                        items[i].put("text", item.getText());
                        Uri uri = item.getUri();
                        items[i].put("uri", uri);

                        JSONObject fileMeta = getFileMetaFromUri(uri);
                        // Merge metadata
                        items[i].put("fileName", fileMeta.optString("name"));
                        items[i].put("fileSize", fileMeta.optLong("size"));
                        items[i].put("lastModified", fileMeta.optLong("modifiedDate"));


                    } catch (JSONException e) {
                        Log.d(pluginName, pluginName + " Error thrown during intent > JSON conversion");
                        Log.d(pluginName, e.getMessage());
                        Log.d(pluginName, Arrays.toString(e.getStackTrace()));
                    }
                }
            }
        }

        try {
            intentJSON = new JSONObject();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (items != null) {
                    intentJSON.put("clipItems", new JSONArray(items));
                }
            }

            intentJSON.put("type", intent.getType());
            intentJSON.put("extras", intent.getExtras());
            intentJSON.put("action", intent.getAction());
            intentJSON.put("categories", intent.getCategories());
            intentJSON.put("flags", intent.getFlags());
            intentJSON.put("component", intent.getComponent());
            intentJSON.put("data", intent.getData());
            intentJSON.put("package", intent.getPackage());

            return intentJSON;
        } catch (JSONException e) {
            Log.d(pluginName, pluginName + " Error thrown during intent > JSON conversion");
            Log.d(pluginName, e.getMessage());
            Log.d(pluginName, Arrays.toString(e.getStackTrace()));

            return null;
        }
    }


    public boolean getRealPathFromContentUrl(final JSONArray data, final CallbackContext context) {

        if(!(data.length() == 1)) {
            Log.i("Data length ", String.valueOf(data.length()));
            context.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        }
        try{
            FileUtils fileUtils = new FileUtils(this.cordova.getContext());
            String result = fileUtils.getPath(Uri.parse(data.getString(0)));
            context.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
            return true;
        }catch(Exception e){
            context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
            return false;
        }
        // Previous approach, for older devices
        /*
        ContentResolver cR = this.cordova.getActivity().getApplicationContext().getContentResolver();
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = cR.query(Uri.parse(data.getString(0)),  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            context.sendPluginResult(new PluginResult(PluginResult.Status.OK, cursor.getString(column_index)));
            return true;
        }catch (Exception e){
            Log.d(pluginName, pluginName + " Error thrown during intent resolve ");
            Log.d(pluginName, e.getMessage());
        } finally{
            if (cursor != null) {
                cursor.close();
            }

            context.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        }
        */
    }

    private JSONObject getFileMetaFromUri(Uri uri) {
        JSONObject meta = new JSONObject();
        try {
            // File name
            String name = null;
            long size = -1;
            long modDate = 0;

            if ("file".equalsIgnoreCase(uri.getScheme())) {
                File file = new File(Objects.requireNonNull(uri.getPath()));
                name = file.getName();
                size = file.length();
                modDate = file.lastModified();
            } else {
                Cursor cursor = cordova.getActivity().getContentResolver()
                        .query(uri, null, null, null, null);
                if (cursor != null) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex);
                    }

                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }

                    // Try common modified date columns
                    int modIndex = cursor.getColumnIndex("last_modified");
                    if (modIndex == -1) {
                        modIndex = cursor.getColumnIndex("date_modified");
                    }
                    if (modIndex != -1) {
                        modDate = cursor.getLong(modIndex);
                    }

                    cursor.close();
                }
            }

            // Fallbacks
            if (modDate <= 0) {
                modDate = System.currentTimeMillis();
            }

            meta.put("uri", uri.toString());
            if (name != null) meta.put("name", name);
            if (size >= 0) meta.put("size", size);
            meta.put("modifiedDate", modDate);

        } catch (Exception e) {
            Log.d(pluginName, "Error building file metadata: " + e.getMessage());
        }
        return meta;
    }
}