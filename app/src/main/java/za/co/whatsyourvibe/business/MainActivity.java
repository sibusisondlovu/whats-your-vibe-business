package za.co.whatsyourvibe.business;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;

import za.co.whatsyourvibe.business.activities.profile.MembershipActivity;
import za.co.whatsyourvibe.business.activities.event.EventCategory;
import za.co.whatsyourvibe.business.adapters.EventsAdapter;
import za.co.whatsyourvibe.business.models.MyEvent;

public class MainActivity extends AppCompatActivity {
    private ProgressBar mProgressBar;
    private TextView mMessage;
    private RecyclerView mRecyclerView;
    private EventsAdapter mAdapter;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;
    private Toolbar toolbar;
    private String mDisplayName;
    private String mMembershipType;
    private String businessId;
    private FirebaseAuth auth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        auth = FirebaseAuth.getInstance();

        businessId = auth.getUid();

        getSupportActionBar().setTitle("Whats Your Vibe");

        mDisplayName = auth.getCurrentUser().getDisplayName();

        setNavigationDrawer();

        mRecyclerView = findViewById(R.id.main_recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);

        mProgressBar = findViewById(R.id.main_progressBar);
        mMessage = findViewById(R.id.main_tvMessage);


        FloatingActionButton fabCreateEvent = findViewById(R.id.main_fabCreateEvent);

        fabCreateEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, EventCategory.class);
                startActivity(i);
            }
        });

        getMyEvents(businessId);
    }


    private void setNavigationDrawer() {
        mDrawerLayout = findViewById(R.id.main_navigationView);
        NavigationView mNavigationView = findViewById(R.id.home_nav_view);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open,
                R.string.drawer_close);

        View hView =  mNavigationView.getHeaderView(0);
        final TextView nav_user = hView.findViewById(R.id.home_drawer_header_tvDisplayName);
        nav_user.setText(mDisplayName);

        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()){
                    case R.id.home_drawer_logout:

                        auth.signOut();

                        finish();

                        Toast.makeText(MainActivity.this, "Thank you for using Whats Your Vibe",
                                Toast.LENGTH_LONG).show();

                        mDrawerLayout.closeDrawers();
                        break;

                    case R.id.home_drawer_membership:
                        Intent i = new Intent(MainActivity.this, MembershipActivity.class);
                        startActivity(i);
                        mDrawerLayout.closeDrawers();
                        break;

                }
                return false;
            }
        });

    }

    private void getMyEvents(String businessId) {

        mProgressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection("events")
                .whereEqualTo("creatorUid",businessId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {

                        if (e != null) {

                            mMessage.setText(e.getMessage());

                            mMessage.setVisibility(View.VISIBLE);

                            mProgressBar.setVisibility(View.GONE);

                            return;
                        }

                        mProgressBar.setVisibility(View.GONE);

                        if (queryDocumentSnapshots.isEmpty()) {
                            mMessage.setText("You do not have any events created. Tap create " +
                                    "event button bottom right of your screen");

                            mMessage.setVisibility(View.VISIBLE);

                            mProgressBar.setVisibility(View.GONE);

                            mRecyclerView.setVisibility(View.GONE);

                        }else{

                            List<MyEvent> myEventList = new ArrayList<>();

                            for (DocumentSnapshot doc: queryDocumentSnapshots){

                               // MyEvent myEvent = doc.toObject(MyEvent.class);

                                MyEvent myEvent1 = doc.toObject(MyEvent.class);

                                myEventList.add(myEvent1);

                            }

                            mAdapter = new EventsAdapter(myEventList,getApplicationContext());

                            mRecyclerView.setAdapter(mAdapter);

                            mAdapter.notifyDataSetChanged();

                            mRecyclerView.setVisibility(View.VISIBLE);
                        }

                    }
                });
    }
}
