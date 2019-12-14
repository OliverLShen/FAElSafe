package edu.ucsb.cs.cs184.oshen.faelsafe;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import android.os.Environment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import android.graphics.Color;
import android.view.View;
import android.*;
import android.widget.Toast;


public class MainActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    class TextAdapter extends BaseAdapter {
        private boolean[] selection;
        private List<String> files = new ArrayList<>();
        public void setData(List<String> data){
            if(data != null){
                this.files.clear();
                if(data.size() > 0 ){
                    this.files.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        void setSelection(boolean[] selection){
            if (selection != null){
                this.selection = new boolean[selection.length];
                for(int i = 0; i < selection.length; i++){
                    this.selection[i] = selection[i];

                }
                notifyDataSetChanged();
            }

        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public String getItem(int i) {
            return files.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.retrieved_files, viewGroup,false);
                view.setTag(new ViewHolder((TextView) view.findViewById(R.id.files)));
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            final String item = getItem(i);
            holder.info.setText(item);
            //holder.info.setText(item.substring(item.lastIndexOf('/')+1));
            if (selection!=null){
                if(selection[i]){
                    holder.info.setBackgroundColor(Color.GREEN);
                }
                else{
                    holder.info.setBackgroundColor(Color.WHITE);
                }
                boolean isSelected = false;
                for(boolean bool: selection){
                    if(bool == true){
                        isSelected=true;
                        break;
                    }
                }
                if(isSelected){
                    findViewById(R.id.encrypt).setVisibility(View.VISIBLE);
                }
                else{
                    findViewById(R.id.encrypt).setVisibility(View.INVISIBLE);
                }
            }

            return view;
        }
    }

    class ViewHolder{
        TextView info;
        ViewHolder(TextView info){
            this.info = info;
        }
    }


    private static final int REQUEST_PERMISSIONS = 1234;
    private EncryptorSgltn encryptor = EncryptorSgltn.INSTANCE;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_COUNT = 2;

    private boolean arePermissionsDenied(){
        int p = 0;
        while (p < REQUEST_COUNT){
            if(checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED){
                return true;
            }
            p++;
        }
        return false;
    }

    private boolean arePermissionsGranted(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            int i = 0;
            while (i <REQUEST_COUNT){
                if(checkSelfPermission(PERMISSIONS[i])!= PackageManager.PERMISSION_GRANTED){
                    return true;
                }
                i++;
            }
        }
        return false;
    }


    private boolean flagInitialized = false;

    private boolean[] fileSelected;


    @Override
    protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        if(!flagInitialized){
            final String filePath = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/Camera";
            //final String filePath = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());

            final File dir = new File(filePath);
            final File[] files = dir.listFiles();
            final int filesFound = files.length;
            final ListView lister = findViewById(R.id.listview);
            final TextAdapter texter = new TextAdapter();
            lister.setAdapter(texter);
            List<String> test = new ArrayList<>();
            for(int i = 0; i < filesFound; i++){
                test.add(String.valueOf(files[i].getAbsolutePath()));
            }
            texter.setData(test);

            fileSelected = new boolean[files.length];
            lister.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    fileSelected[i] = !fileSelected[i];
                    texter.setSelection(fileSelected);
                    return false;
                }
            });

            final Button button = findViewById(R.id.encrypt);
            final Button encryptedFiles =findViewById(R.id.encyptedFiles);
            encryptedFiles.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendMessage(view);
                }
            });
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder encryptDialog = new AlertDialog.Builder(MainActivity2.this);
                    encryptDialog.setTitle("Encrypt");
                    encryptDialog.setMessage("Do you wish to encrypt the selected file?");
                    encryptDialog.setPositiveButton("Encrypt", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            encryptor.init(getApplicationContext());
                            for(int j = 0; j < fileSelected.length; j++){
                                if (fileSelected[j] == true) {
                                    boolean encWork = encryptor.encryptFile(files[j].getAbsolutePath());
                                    if(encWork){
                                        Context context = getApplicationContext();
                                        CharSequence text = "File Successfully Encrypted!";
                                        int duration = Toast.LENGTH_SHORT;

                                        Toast toast = Toast.makeText(context, text, duration);
                                        toast.show();
                                        File file = new File(files[j].getAbsolutePath());
                                        //boolean flag = file.delete();
                                        //context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

                                        //choosePath();
                                        //deletefile(uri,files[j].getAbsolutePath());
                                        //deleteFileorFiles(files[i].getAbsoluteFile());
                                    }
                                    else {
                                        Context context = getApplicationContext();
                                        CharSequence text = "File Encryption Failed";
                                        int duration = Toast.LENGTH_SHORT;

                                        Toast toast = Toast.makeText(context, text, duration);
                                        toast.show();
                                    }
                                }
                            }
                        }
                    });

                    encryptDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    encryptDialog.show();
                }
            });
            flagInitialized = true;

        }
    }





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSIONS && grantResults.length > 0){
            if(arePermissionsDenied()){
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }
        }
    }

    private static final int LOCATION_REQUEST = 1;

    private void choosePath() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);


    }
    public void sendMessage(View view)
    {
        Intent intent = new Intent(MainActivity2.this, EncryptedFiles.class);
        startActivity(intent);
    }
/*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode,resultCode,resultData);
        if (requestCode == LOCATION_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                Log.d("DEBUG", uri.getLastPathSegment());
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        String path = FileUtil.getFullPathFromTreeUri(uri, this);
                        deletefile(uri, path);

                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private void deletefile(Uri uri, String filename) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, uri);
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        DocumentFile file = pickedDir.findFile(filename);
        if(file.delete())
            Log.d("Log ID", "Delete successful");
        else
            Log.d("Log ID", "Delete unsuccessful");
    }*/
}


