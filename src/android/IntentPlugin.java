package com.betasoft.cordova.plugin.intent;

import java.io.File;
import java.io.FileDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

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
    private JSONObject getIntentJson1(Intent intent) {
        JSONObject intentJSON = null;
        ClipData clipData = null;
        JSONObject[] items = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            clipData = intent.getClipData();
            if(clipData != null) {
                int clipItemCount = clipData.getItemCount();
                items = new JSONObject[clipItemCount];

                for (int i = 0; i < clipItemCount; i++) {

                    ClipData.Item item = clipData.getItemAt(i);

                    try {
                        items[i] = new JSONObject();
                        items[i].put("htmlText", item.getHtmlText());
                        items[i].put("intent", item.getIntent());
                        items[i].put("text", item.getText());
                        items[i].put("uri", item.getUri());
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
                if(items != null) {
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

    private JSONObject getIntentJson(Intent intent) {
        JSONObject intentJSON = new JSONObject();
        JSONArray clipItemsArray = new JSONArray();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        JSONObject itemJson = new JSONObject();

                        Uri uri = item.getUri();
                        itemJson.put("htmlText", item.getHtmlText());
                        itemJson.put("intent", item.getIntent());
                        itemJson.put("text", item.getText());
                        itemJson.put("uri", uri != null ? uri.toString() : null);
                        //itemJson.put("fileName", getFileName(uri));
                        JSONObject fileInfo = getFileInfo(uri);
                        itemJson.put("fileName", fileInfo.optString("fileName"));
                        itemJson.put("fileSize", fileInfo.optLong("fileSize"));
                        itemJson.put("lastModified", fileInfo.optLong("lastModified"));

                        clipItemsArray.put(itemJson);
                    }
                }
            }

            // Fallback for older versions or single data URI
            Uri dataUri = intent.getData();
            if (dataUri != null) {
                JSONObject itemJson = new JSONObject();
                itemJson.put("uri", dataUri.toString());
                //itemJson.put("fileName", getFileName(dataUri));
                JSONObject fileInfo = getFileInfo(dataUri);
                itemJson.put("fileName", fileInfo.optString("fileName"));
                itemJson.put("fileSize", fileInfo.optLong("fileSize"));
                itemJson.put("lastModified", fileInfo.optLong("lastModified"));
                clipItemsArray.put(itemJson);
            }

            intentJSON.put("clipItems", clipItemsArray);
            intentJSON.put("type", intent.getType());
            intentJSON.put("extras", intent.getExtras());
            intentJSON.put("action", intent.getAction());
            intentJSON.put("categories", intent.getCategories());
            intentJSON.put("flags", intent.getFlags());
            intentJSON.put("component", intent.getComponent());
            intentJSON.put("data", intent.getData() != null ? intent.getData().toString() : null);
            intentJSON.put("package", intent.getPackage());

        } catch (JSONException e) {
            Log.e(pluginName, "Error building intent JSON", e);
        }

        return intentJSON;
    }

    private String getFileName(Uri uri) {
        if (uri == null) return null;

        String fileName = null;

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            Context context = this.cordova.getContext();
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex);
                    }
                } catch (Exception e) {
                    Log.e(pluginName, "Error reading file name from content URI", e);
                } finally {
                    cursor.close();
                }
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            fileName = new File(Objects.requireNonNull(uri.getPath())).getName();
        }

        return fileName;
    }


    private JSONObject getFileInfo(Uri uri) {
        JSONObject fileInfo = new JSONObject();

        if (uri == null) return fileInfo;

        try {
            String fileName = null;
            long fileSize = -1;
            long lastModified = -1;

            if ("content".equalsIgnoreCase(uri.getScheme())) {
                // Use DocumentFile for SAF-compatible URIs
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    DocumentFile docFile = DocumentFile.fromSingleUri(this.cordova.getContext(), uri);
                    if (docFile != null) {
                        fileName = docFile.getName();
                        fileSize = docFile.length();
                        lastModified = docFile.lastModified();
                    }
                }

                // Fallback to ContentResolver if DocumentFile fails
                if (fileName == null) {
                    Cursor cursor = this.cordova.getContext().getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null) {
                        try {
                            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                            if (cursor.moveToFirst()) {
                                if (nameIndex != -1) {
                                    fileName = cursor.getString(nameIndex);
                                }
                                if (sizeIndex != -1) {
                                    fileSize = cursor.getLong(sizeIndex);
                                }
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                }

            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                File file = new File(Objects.requireNonNull(uri.getPath()));
                fileName = file.getName();
                fileSize = file.length();
                lastModified = file.lastModified();
            }

            fileInfo.put("fileName", fileName);
            fileInfo.put("fileSize", fileSize);
            fileInfo.put("lastModified", lastModified);

        } catch (Exception e) {
            Log.e(pluginName, "Error extracting file info", e);
        }

        return fileInfo;
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

}