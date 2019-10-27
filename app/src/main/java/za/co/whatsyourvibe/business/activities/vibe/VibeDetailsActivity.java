package za.co.whatsyourvibe.business.activities.vibe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import za.co.whatsyourvibe.business.R;
import za.co.whatsyourvibe.business.activities.event.EventCategory;
import za.co.whatsyourvibe.business.activities.event.EventOptionsActivity;
import za.co.whatsyourvibe.business.adapters.ImagesAdapter;
import za.co.whatsyourvibe.business.models.Image;
import za.co.whatsyourvibe.business.models.Vibe;

public class VibeDetailsActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener{

    private static final String TAG = "VibeDetailsActivity";

    private int AUTOCOMPLETE_REQUEST_CODE = 1;

    private static final int RC_PERMISSION = 200;

    private TextView mDescription, mCategory, mLocation, mDate, mTime;

    private TextView mTicket;

    private TextView mRestrictions;

    private TextView mPhotos, mVideos;

    private String mVibeLocationId;

    String vibeType = "Free Event";

    String standard = "";

    String earlyBird = "";

    String group = "";

    String vip = "";

    private Uri mImageUri;

    private StorageReference mStorageReference;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        String vibeId = getIntent().getStringExtra("VIBE_ID");

        String vibeTitle = getIntent().getStringExtra("VIBE_TITLE");

        setContentView(R.layout.activity_vibe_details);

        Toolbar toolbar = findViewById(R.id.vibes_details_toolbar);

        mStorageReference = FirebaseStorage.getInstance().getReference("event_photos");

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button addButton = findViewById(R.id.toolbar_createVibe);

        addButton.setVisibility(View.GONE);

        String apiKey = getString(R.string.api_key);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        if (vibeId !=null) {

            getVibeDetails(vibeId);

            getSupportActionBar().setTitle(vibeTitle);

            mVibeLocationId = vibeId;

            initViews(vibeId);

        }else {

            Toast.makeText(VibeDetailsActivity.this, "Couldn't load vibe details " +
                                                             "at this time",
                    Toast.LENGTH_LONG).show();

            finish();

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {

            if (resultCode == RESULT_OK) {

                Place place = Autocomplete.getPlaceFromIntent(data);



                mLocation.setText(place.getAddress());

                FirebaseFirestore locationRef = FirebaseFirestore.getInstance();

                locationRef.collection("vibes")
                        .document(mVibeLocationId)
                        .update("location", place.getAddress());


            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {

                // TODO: Handle the error.

                Status status = Autocomplete.getStatusFromIntent(data);

                Log.i(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {

                // The user canceled the operation.
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mImageUri = result.getUri();

                uploadImage();

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
            }
        }
    }

    private void initViews(String id) {

        mDescription = findViewById(R.id.vibe_details_tvDescription);

        mCategory = findViewById(R.id.vibe_details_tvCategory);

        mLocation = findViewById(R.id.vibe_details_tvLocation);

        mDate = findViewById(R.id.vibe_details_tvDate);

        mTime = findViewById(R.id.vibe_details_tvTime);

        mTicket = findViewById(R.id.vibe_details_tvTickets);

        mRestrictions = findViewById(R.id.vibe_details_tvRestrictions);

        mPhotos = findViewById(R.id.vibe_details_tvPhotos);

        mVideos = findViewById(R.id.vibe_details_tvVideos);

        // edit buttons

        setViewListeners(id);

    }

    private void setViewListeners(String id) {

        ImageView editDescription = findViewById(R.id.vibe_details_btnEditDescription);

        editDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editDescription(id);
            }
        });

        ImageView editCategory = findViewById(R.id.vibe_details_btnEditCategory);

        editCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editCategoryDialog(id);

            }
        });

        ImageView editLocation = findViewById(R.id.vibe_details_btnEditLocation);

        editLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editLocation(id);

            }
        });

        ImageView editDate = findViewById(R.id.vibe_details_btnEditDate);

        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setEventDate();

            }
        });


        ImageView editTime = findViewById(R.id.vibe_details_btnEditTime);

        editTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setEventTime();

            }
        });

        ImageView editTickets = findViewById(R.id.vibe_details_btnEditTickets);

        editTickets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editTicketsDialog(id);

            }
        });

        ImageView uploadImage = findViewById(R.id.vibe_details_btnUploadImage);

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkPermission();
            }
        });

        ImageView viewImages = findViewById(R.id.vibe_details_btnViewImage);

        viewImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showVibeImages(id);
            }
        });
    }

    private void checkPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_DENIED) {

                // permission not granted. ask for it
                String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};

                // show pop up
                requestPermissions(permissions, RC_PERMISSION);

            } else {

                // permission already granted
                pickImageFromGallery();
            }

        } else {
            // device less then mashmallow

            pickImageFromGallery();

        }

    }

    private void pickImageFromGallery() {

        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(4,3)
                .setMinCropResultSize(450,200)
                .start(this);

    }

    private void editDescription(String id) {

        //show edit dialog box
        editDescriptionDialog(id);

        getVibeDetails(id);
    }

    private void editCategoryDialog(String id) {

        //before inflating the custom alert dialog layout, we will get the current activity viewgroup
        ViewGroup viewGroup = findViewById(android.R.id.content);

        //then we will inflate the custom alert dialog xml that we created
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_vibe_category, viewGroup, false);


        FirebaseFirestore categoriesRef = FirebaseFirestore.getInstance();

        List<String> categoriesList = new ArrayList<>();

        final AutoCompleteTextView actCategories =
                dialogView.findViewById(R.id.dialog_update_vibe_category_actCategory);

        actCategories.setHint("Loading categories....");

        categoriesRef.collection("categories")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {

                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots){

                                categoriesList.add(doc.get("title").toString());


                            }

                            ArrayAdapter<String> categoriesAdapter =
                                    new ArrayAdapter<>(VibeDetailsActivity.this,
                                            android.R.layout.simple_list_item_1,
                                            categoriesList);

                            actCategories.setAdapter(categoriesAdapter);
                            actCategories.setHint("Type category name");

                        }else{
                            actCategories.setHint("No categories found");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        actCategories.setError(e.getMessage());
                    }
                });


        final Button btnUpdate = dialogView.findViewById(R.id.dialog_update_vibe_category_btnUpdate);

        //Now we need an AlertDialog.Builder object
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //setting the view of the builder to our custom view that we already inflated
        builder.setView(dialogView);

        //finally creating the alert dialog and displaying it
        final AlertDialog alertDialog = builder.create();

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                btnUpdate.setEnabled(false);

                btnUpdate.setText("Updating category...");


                FirebaseFirestore vibesRef = FirebaseFirestore.getInstance();

                vibesRef.collection("vibes")
                        .document(id)
                        .update("category", actCategories.getText().toString())
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                alertDialog.dismiss();

                                Toast.makeText(VibeDetailsActivity.this, e.getMessage(),
                                        Toast.LENGTH_SHORT).show();

                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                alertDialog.dismiss();

                                // redirect to Vibe Detail Screen

                                Toast.makeText(VibeDetailsActivity.this, "Category updated",
                                        Toast.LENGTH_SHORT).show();

                                getVibeDetails(id);


                            }
                        });

            }

        });

        alertDialog.show();

    }

    private void editDescriptionDialog(String id) {

        //before inflating the custom alert dialog layout, we will get the current activity viewgroup
        ViewGroup viewGroup = findViewById(android.R.id.content);

        //then we will inflate the custom alert dialog xml that we created
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_vibe_description, viewGroup, false);


        final TextInputLayout description =
                dialogView.findViewById(R.id.dialog_edit_vibe_description);

        description.getEditText().setText(mDescription.getText().toString());

        final Button btnUpdate = dialogView.findViewById(R.id.dialog_edit_vibe_btnCreate);

        //Now we need an AlertDialog.Builder object
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //setting the view of the builder to our custom view that we already inflated
        builder.setView(dialogView);

        //finally creating the alert dialog and displaying it
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();


        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                btnUpdate.setEnabled(false);

                btnUpdate.setText("Updating description...");


                FirebaseFirestore vibesRef = FirebaseFirestore.getInstance();

                vibesRef.collection("vibes")
                        .document(id)
                        .update("description", description.getEditText().getText().toString())
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                alertDialog.dismiss();

                                Toast.makeText(VibeDetailsActivity.this, e.getMessage(),
                                        Toast.LENGTH_SHORT).show();

                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                alertDialog.dismiss();

                                // redirect to Vibe Detail Screen

                                Toast.makeText(VibeDetailsActivity.this, "Description updated",
                                        Toast.LENGTH_SHORT).show();


                            }
                        });

            }
        });
    }

    private void editLocation(String id) {

        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS);

        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields)
                                .setCountry("ZA")
                                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);

    }

    private void editTicketsDialog(String id) {


        //before inflating the custom alert dialog layout, we will get the current activity viewgroup
        ViewGroup viewGroup = findViewById(android.R.id.content);

        //then we will inflate the custom alert dialog xml that we created
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_vibe_tickets, viewGroup, false);


        final Switch type =
                dialogView.findViewById(R.id.dialog_update_vibe_tickets_swTicket);



        CheckBox cbStandard = dialogView.findViewById(R.id.dialog_update_vibe_tickets_cbStandard);

        CheckBox cbEarlyBird = dialogView.findViewById(R.id.dialog_update_vibe_tickets_cbEarlyBird);

        CheckBox cbVIP = dialogView.findViewById(R.id.dialog_update_vibe_tickets_cbVip);

        CheckBox cbGroup = dialogView.findViewById(R.id.dialog_update_vibe_tickets_cbGroup);

        EditText etStandard = dialogView.findViewById(R.id.dialog_update_vibe_tickets_etStandard);

        EditText etEarlyBird = dialogView.findViewById(R.id.dialog_update_vibe_tickets_etEarlyBird);

        EditText etGroup = dialogView.findViewById(R.id.dialog_update_vibe_tickets_etGroup);

        EditText etVIP = dialogView.findViewById(R.id.dialog_update_vibe_tickets_etVip);

        final Button btnUpdate = dialogView.findViewById(R.id.dialog_update_vibe_tickets_btnUpdate);

        //Now we need an AlertDialog.Builder object
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //setting the view of the builder to our custom view that we already inflated
        builder.setView(dialogView);

        //finally creating the alert dialog and displaying it
        final AlertDialog alertDialog = builder.create();

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (type.isChecked()) {

                    btnUpdate.setEnabled(false);

                    btnUpdate.setText("Updating tickets...");


                    FirebaseFirestore vibesRef = FirebaseFirestore.getInstance();

                    HashMap<String, Object> ticketsRef = new HashMap<>();

                    ticketsRef.put("type", vibeType);

                    ticketsRef.put("standard", etStandard.getText().toString());

                    ticketsRef.put("earlyBird", etEarlyBird.getText().toString());

                    ticketsRef.put("group", etGroup.getText().toString());

                    ticketsRef.put("vip", etVIP.getText().toString());

                    vibesRef.collection("vibes")
                            .document(id)
                            .update(ticketsRef);

                    alertDialog.dismiss();

                    mTicket.setText("Early Bird: R" + etEarlyBird.getText().toString() +
                                            "\nStandard: R" + etStandard.getText().toString() +
                                            "\nVIP: R" + etVIP.getText().toString()+ "\nGroup: R" + etGroup.getText().toString());

                }else {

                    mTicket.setText("Free Event");
                }

            }
        });

        type.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    cbStandard.setVisibility(View.VISIBLE);

                    cbEarlyBird.setVisibility(View.VISIBLE);

                    cbVIP.setVisibility(View.VISIBLE);

                    cbGroup.setVisibility(View.VISIBLE);

                    vibeType = "Paid Event";

                }else {

                    cbStandard.setVisibility(View.GONE);

                    cbEarlyBird.setVisibility(View.GONE);

                    cbVIP.setVisibility(View.GONE);

                    cbGroup.setVisibility(View.GONE);

                    vibeType = "Free Event";

                }
            }
        });

        cbStandard.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    etStandard.setVisibility(View.VISIBLE);

                    standard = etStandard.getText().toString();


                }else {

                    etStandard.setVisibility(View.GONE);

                    etStandard.setText("");

                    standard = "";

                }

            }
        });

        cbEarlyBird.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    etEarlyBird.setVisibility(View.VISIBLE);

                    earlyBird = etEarlyBird.getText().toString();

                }else {

                    etEarlyBird.setVisibility(View.GONE);

                    etEarlyBird.setText("");

                    earlyBird = "";

                }

            }
        });

        cbVIP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    etVIP.setVisibility(View.VISIBLE);

                    vip = etVIP.getText().toString();

                }else {

                    etVIP.setVisibility(View.GONE);

                    etVIP.setText("");

                    vip = "";


                }

            }
        });

        cbGroup.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    etGroup.setVisibility(View.VISIBLE);


                    group = etGroup.getText().toString();


                }else {

                    etGroup.setVisibility(View.GONE);

                    etGroup.setText("");

                    group = "";

                }

            }
        });

        alertDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR,year);

        c.set(Calendar.MONTH,month);

        c.set(Calendar.DAY_OF_MONTH,dayOfMonth);

        String date = DateFormat.getDateInstance(DateFormat.FULL).format(c.getTime());

        mDate.setText(date);

        FirebaseFirestore dateRef = FirebaseFirestore.getInstance();

        dateRef.collection("vibes")
                .document(mVibeLocationId)
                .update("date",date);


    }

    private void setEventDate() {

        DialogFragment eventDate = new za.co.whatsyourvibe.business.utils.DatePicker();

        eventDate.show(getSupportFragmentManager(),"EVENT_DATE");
    }

    private void setEventTime() {

        DialogFragment eventTime = new za.co.whatsyourvibe.business.utils.TimePicker();

        eventTime.show(getSupportFragmentManager(),"EVENT_TIME");
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        mTime.setText(hourOfDay + " : " + minute);

        FirebaseFirestore timeRef = FirebaseFirestore.getInstance();

        timeRef.collection("vibes")
                .document(mVibeLocationId)
                .update("time",hourOfDay + " : " + minute);

    }

    // gt vibe description
    private void getVibeDetails(String id) {

        FirebaseFirestore vibeRef = FirebaseFirestore.getInstance();

        vibeRef.collection("vibes")
                .document(id)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        if (documentSnapshot.exists()) {

                            Vibe vibe = documentSnapshot.toObject(Vibe.class);

                            if (vibe !=null) {

                                mDescription.setText(vibe.getDescription());

                                mCategory.setText(vibe.getCategory());

                                mLocation.setText(vibe.getLocation());

                                mDate.setText(vibe.getDate());

                                mTime.setText(vibe.getTime());

                                if (TextUtils.isEmpty(vibe.getType())) {

                                    mTicket.setText("No tickets set for this event");

                                }else {

                                    mTicket.setText("Early Bird: R" + vibe.getEarlyBird() +
                                                            "\nStandard: R" + vibe.getStandard() +
                                                            "\nVIP: R" +  vibe.getVip()+ "\nGroup" +
                                                            ": R" + vibe.getGroup());

                                }


                                mRestrictions.setText("Unrestricted vibe");

                                mPhotos.setText("No photos uploaded");

                                mVideos.setText("No video uploaded");

                            }


                        }else{

                            Toast.makeText(VibeDetailsActivity.this, "Couldn't load vibe details " +
                                                                             "at this time",
                                    Toast.LENGTH_LONG).show();

                            finish();
                        }


                    }
                });

    }

    private void uploadImage() {

        ProgressDialog dialog = new ProgressDialog(this);

        dialog.setMessage("Uploading Image...");

        dialog.show();

        final StorageReference fileReference =
                mStorageReference.child(mVibeLocationId + Calendar.getInstance().getTimeInMillis());


        UploadTask uploadTask = fileReference.putFile(mImageUri);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                dialog.dismiss();

                Toast.makeText(VibeDetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                Log.e(TAG, "onFailure: " +  e.getMessage());

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        updateImageLink(uri);

                        dialog.dismiss();

                        Toast.makeText(VibeDetailsActivity.this, "Image uploaded successfully",
                                Toast.LENGTH_SHORT).show();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        dialog.dismiss();

                        Toast.makeText(VibeDetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

            }
        });

    }

    private void updateImageLink(Uri uri) {

        FirebaseFirestore imagesRef = FirebaseFirestore.getInstance();

        HashMap<String, Object> image = new HashMap<>();

        image.put("downloadLink", uri.toString());

        imagesRef.collection("photos")
                .document(mVibeLocationId)
                .collection("vibePhotos")
                .add(image);
    }

    private void showVibeImages(String id) {

        //before inflating the custom alert dialog layout, we will get the current activity viewgroup
        ViewGroup viewGroup = findViewById(android.R.id.content);

        //then we will inflate the custom alert dialog xml that we created
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_vibe_images, viewGroup, false);


        final RecyclerView recyclerView =
                dialogView.findViewById(R.id.dialog_vibe_images_recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ProgressBar progressBar = dialogView.findViewById(R.id.dialog_vibe_images_progressBar);

        TextView textView = dialogView.findViewById(R.id.dialog_vibe_images_textView);


        final ImageView btnClose = dialogView.findViewById(R.id.dialog_vibe_images_btnClose);

        FirebaseFirestore imagesRef = FirebaseFirestore.getInstance();

        imagesRef.collection("photos")
                .document(mVibeLocationId)
                .collection("vibePhotos")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        if (queryDocumentSnapshots !=null && !queryDocumentSnapshots.isEmpty()) {

                            List<Image> images = new ArrayList<>();

                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {

                                Image image = doc.toObject(Image.class);

                                images.add(image);
                            }

                            ImagesAdapter adapter = new ImagesAdapter(images,
                                    getApplicationContext());

                            recyclerView.setAdapter(adapter);

                            adapter.notifyDataSetChanged();

                            progressBar.setVisibility(View.GONE);

                            recyclerView.setVisibility(View.VISIBLE);

                            textView.setVisibility(View.GONE);


                        }else {

                            progressBar.setVisibility(View.GONE);

                            recyclerView.setVisibility(View.GONE);

                            textView.setText("No images uploaded");

                            textView.setVisibility(View.VISIBLE);
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        progressBar.setVisibility(View.GONE);

                        recyclerView.setVisibility(View.GONE);

                        textView.setText(e.getMessage());

                        textView.setVisibility(View.VISIBLE);
                    }
                });


        //Now we need an AlertDialog.Builder object
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //setting the view of the builder to our custom view that we already inflated
        builder.setView(dialogView);

        //finally creating the alert dialog and displaying it
        final AlertDialog alertDialog = builder.create();

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                alertDialog.dismiss();

            }
        });

        alertDialog.show();

    }
}