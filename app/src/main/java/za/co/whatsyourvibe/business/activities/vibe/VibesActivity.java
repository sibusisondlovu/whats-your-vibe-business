package za.co.whatsyourvibe.business.activities.vibe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import za.co.whatsyourvibe.business.R;
import za.co.whatsyourvibe.business.adapters.VibesAdapter;
import za.co.whatsyourvibe.business.models.Vibe;

public class VibesActivity extends AppCompatActivity {

    private static final String TAG = "VibesActivity";

    private RecyclerView recyclerView;

    private ProgressBar progressBar;

    private TextView textView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vibes);

        Toolbar toolbar = findViewById(R.id.vibes_toolbar);

        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("WYV");

        Button btnCreateVibe = findViewById(R.id.toolbar_createVibe);


        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        initViews();

        if (mAuth.getCurrentUser() !=null) {

            getVibes(mAuth.getCurrentUser().getUid());

            btnCreateVibe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    showCreateVibeDialog(mAuth.getCurrentUser().getUid());

                }
            });

        }


    }

    private void showCreateVibeDialog(String currentUserId) {

        //before inflating the custom alert dialog layout, we will get the current activity viewgroup
        ViewGroup viewGroup = findViewById(android.R.id.content);

        //then we will inflate the custom alert dialog xml that we created
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_vibe, viewGroup, false);


        final TextInputLayout title =
                dialogView.findViewById(R.id.dialog_create_vibe_title);

        final TextInputLayout description =
                dialogView.findViewById(R.id.dialog_create_vibe_description);

        final Spinner category = dialogView.findViewById(R.id.dialog_create_vibe_spnCategory);


        final Button btnCreate = dialogView.findViewById(R.id.dialog_create_vibe_btnCreate);


        //Now we need an AlertDialog.Builder object
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //setting the view of the builder to our custom view that we already inflated
        builder.setView(dialogView);

        //finally creating the alert dialog and displaying it
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();


        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(title.getEditText().getText())) {

                    title.getEditText().setText ("Title is requires");

                    title.requestFocus();

                    return;

                }


                btnCreate.setEnabled(false);

                btnCreate.setText("Creating vibe...");
                // create a profile to database

                String vibeId =  "new Date()";

                HashMap<String, Object> basicVibeInformation = new HashMap<>();

                basicVibeInformation.put("id", vibeId);

                basicVibeInformation.put("title", title.getEditText().getText().toString());

                basicVibeInformation.put("organiserId", currentUserId);

                basicVibeInformation.put("description",
                        description.getEditText().getText().toString());

                basicVibeInformation.put("category", "No category selected");


                FirebaseFirestore vibesRef = FirebaseFirestore.getInstance();

                vibesRef.collection("vibes")
                        .document(vibeId)
                        .set(basicVibeInformation)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                alertDialog.dismiss();

                                Toast.makeText(VibesActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                alertDialog.dismiss();

                                // redirect to Vibe Detail Screen

                                Toast.makeText(VibesActivity.this, "Vibe created successfully",
                                        Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(VibesActivity.this,
                                        VibeDetailsActivity.class);

                                startActivity(intent);

                            }
                        });

            }
        });

    }

    private void initViews() {

        textView = findViewById(R.id.vibes_textView);

        progressBar = findViewById(R.id.vibes_progressBar);

        recyclerView = findViewById(R.id.vibes_recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setHasFixedSize(true);
    }

    private void getVibes(String currentUser) {

        FirebaseFirestore vibesRef = FirebaseFirestore.getInstance();

        vibesRef.collection("vibes")
                .whereEqualTo("organiserId",currentUser)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                        @Nullable FirebaseFirestoreException e) {

                        if (e !=null) {

                            recyclerView.setVisibility(View.GONE);

                            textView.setVisibility(View.VISIBLE);

                            progressBar.setVisibility(View.GONE);

                            textView.setText(e.getMessage());

                            return;

                        }

                        if (queryDocumentSnapshots !=null && !queryDocumentSnapshots.isEmpty()) {


                            List<Vibe> vibeList = new ArrayList<>();

                            for (QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots) {

                                Vibe vibe = documentSnapshot.toObject(Vibe.class);

                                vibeList.add(vibe);

                            }

                            // hook up adapter
                            VibesAdapter adapter = new VibesAdapter(vibeList,
                                    getApplicationContext());

                            recyclerView.setAdapter(adapter);

                            adapter.notifyDataSetChanged();

                            recyclerView.setVisibility(View.VISIBLE);

                            textView.setVisibility(View.GONE);

                            progressBar.setVisibility(View.GONE);


                        }else{

                            recyclerView.setVisibility(View.GONE);

                            textView.setVisibility(View.VISIBLE);

                            progressBar.setVisibility(View.GONE);

                            textView.setText("Couldn't retrieve Vibes at this time. Try again " +
                                                     "later");

                        }

                    }
                });
    }
}