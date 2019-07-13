package za.co.whatsyourvibe.business.activities.event;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;


import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import za.co.whatsyourvibe.business.R;


public class EventRestrictionsActivity extends AppCompatActivity {

    private static final String TAG = "EventRestrictionsActivi";

    private Switch smoking, children,alcohol;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_restrictions);

        Toolbar toolbar = findViewById(R.id.event_restrictions_toolbar);

        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Event Restrictions");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        smoking = findViewById(R.id.event_restrictions_swSmoking);
        children = findViewById(R.id.event_restrictions_swChildren);
        alcohol = findViewById(R.id.event_restrictions_swAlcohol);

        Button btnNext = findViewById(R.id.event_restrictions_btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextScreen();
            }
        });
    }

    private void nextScreen() {
        EventCategory.myEvent.setSmokingAllowed(smoking.isChecked());
        EventCategory.myEvent.setAlcoholAllowed(alcohol.isChecked());
        EventCategory.myEvent.setChildrenAllowed(children.isChecked());

        Intent i = new Intent(EventRestrictionsActivity.this,EventOptionsActivity.class);
        startActivity(i);
    }
}
