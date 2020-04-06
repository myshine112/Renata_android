package com.renata.mentesaudvel;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.renata.mentesaudvel.Adapter.ReadDetailAdapter;
import com.renata.mentesaudvel.Model.ReadItem;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReadingListAdminActivity extends AppCompatActivity {

    String[] itemname = {};
    DatabaseReference databaseReference;
    Button addChildBtn,editTxtBtn;
    ImageView sectionImage;
    ListView listChild;
    EditText nameTV;
    List<ReadItem> readitems= new ArrayList<>();
    private ReadDetailAdapter readDetailAdapter;
    private static final int GalleryPick = 1;
    private ProgressDialog loadingBar;

    private StorageReference userProfileImageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readinglist);

        Bundle extra = getIntent().getExtras();
        String readingID = extra.getString("readindid");


        databaseReference = FirebaseDatabase.getInstance().getReference("Readings").child(readingID);
        userProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        addChildBtn = (Button) findViewById(R.id.buttonAddChild);
        editTxtBtn = (Button) findViewById(R.id.editTxtBtn);
        sectionImage = (ImageView) findViewById(R.id.sectionImage);
        listChild = (ListView) findViewById(R.id.listChild);
        nameTV = (EditText) findViewById(R.id.sectiontitle);

        loadingBar = new ProgressDialog(this);
        nameTV.setEnabled(false);
//        Toast.makeText(ReadingListAdminActivity.this,""+readingID,Toast.LENGTH_SHORT).show();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String imgID = dataSnapshot.child("reading_image").getValue(String.class);
                String name = dataSnapshot.child("reading_name").getValue(String.class);
                if(imgID.equals( "default" )){
                    Picasso.get().load(R.drawable.newitem).into(sectionImage);
                }
                else {
                    Picasso.get().load(imgID).into(sectionImage);
                }
                nameTV.setText(name);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        editTxtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nameTV.isEnabled()){
                    nameTV.setEnabled(false);
                    databaseReference.child("reading_name").setValue(nameTV.getText().toString());
                    editTxtBtn.setText("Edit Title");
                }else{
                    nameTV.setEnabled(true);
                    editTxtBtn.setText("Update");
                }
            }
        });

        addChildBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String id = databaseReference.child("items").push().getKey();
                ReadItem Item = new ReadItem(id, "default","default");
                //Saving the Item
                databaseReference.child("items").child(id).setValue(Item);

                Toast.makeText(ReadingListAdminActivity.this,"An... Item Added.",Toast.LENGTH_SHORT).show();
            }
        });
        sectionImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GalleryPick);
            }
        });

        readDetailAdapter = new ReadDetailAdapter(ReadingListAdminActivity.this, readitems );

        listChild.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
                AlertDialog.Builder alert = new AlertDialog.Builder(ReadingListAdminActivity.this);
                final EditText edittext = new EditText(ReadingListAdminActivity.this);
//                edittext.setInputType( InputType.TYPE_CLASS_TEXT);
                alert.setTitle("Edit Detail...");

                alert.setView(edittext);
                ReadItem Item = readitems.get(position);
                String detail= Item.getreaditem_detail();
                edittext.setText(detail);

                alert.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String updateText = edittext.getText().toString();
                        ReadItem Item = readitems.get(position);
                        String readingID= Item.getreaditem_id();
                        updateItem(readingID,updateText);

                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.cancel();
                    }
                });
                alert.show();
            }
        });

        listChild.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(ReadingListAdminActivity.this);
                builder1.setTitle("Are you sure you want to delete this Item?");
                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ReadItem Item = readitems.get(position);
                                String readingID= Item.getreaditem_id();
                                deleteItem(readingID);
                            }
                        });

                builder1.setNegativeButton(
                        "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.setIcon(android.R.drawable.ic_dialog_alert);
                alert11.show();

                return true;
            }
        });



    }
    @Override
    public void onStart() {
        super.onStart();
        databaseReference.child("items").addValueEventListener(mValueEventListener);

    }

    ValueEventListener mValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            //clearing the previous Item list
            readitems.clear();
            //getting all nodes
            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                //getting Item from firebase console
                ReadItem Item = postSnapshot.getValue(ReadItem.class);

                readitems.add( Item );
            }

            listChild.setAdapter(readDetailAdapter);
//            pd.dismiss();

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }

    };

    private boolean deleteItem(String id) {
        //getting the specified Item reference
        DatabaseReference DeleteReference = databaseReference.child("items").child(id);
        //removing Item
        DeleteReference.removeValue();
        Toast.makeText(ReadingListAdminActivity.this, "Item Deleted", Toast.LENGTH_LONG).show();
        return true;

    }
    private boolean updateItem(String id,String detail) {
        //getting the specified Item reference
        DatabaseReference UpdateReference = databaseReference.child("items").child(id);
        //removing Item
        ReadItem Item = new ReadItem( id, "default", detail);
        //update  Item  to firebase
        UpdateReference.setValue( Item );
        Toast.makeText(ReadingListAdminActivity.this, "Item Updated", Toast.LENGTH_LONG).show();
        return true;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GalleryPick && resultCode == RESULT_OK && data != null) {
            Uri ImageUri = data.getData();
            // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(11, 4)
                    .start(ReadingListAdminActivity.this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                loadingBar.setTitle(R.string.set_profile_image);
                loadingBar.setMessage("Please wait, your profile image is uploading");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                final Uri resultUri = result.getUri();
                final StorageReference filePath = userProfileImageRef.child(UUID.randomUUID().toString() + ".jpg");

                filePath.putFile(resultUri)
                        .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(ReadingListAdminActivity.this.getContentResolver(), resultUri);
                                sectionImage.setImageBitmap(bitmap);
                                // Continue with the task to get the download URL
                                return filePath.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            final String downloadUri = task.getResult().toString();
                            databaseReference.child("reading_image")
                                    .setValue(downloadUri)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // Toast.makeText(SettingsActivity.this, "Image save on database, successfully...", Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            } else {
                                                String message = task.getException().toString();
                                                Toast.makeText(ReadingListAdminActivity.this, "Error : " + message, Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        }
                                    });
                        } else {
                            String message = task.getException().toString();
                            Toast.makeText(ReadingListAdminActivity.this, "Error : " + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}