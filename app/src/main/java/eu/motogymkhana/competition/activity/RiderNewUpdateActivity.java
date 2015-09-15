package eu.motogymkhana.competition.activity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;

import eu.motogymkhana.competition.R;
import eu.motogymkhana.competition.api.impl.RidersCallback;
import eu.motogymkhana.competition.model.Bib;
import eu.motogymkhana.competition.model.Country;
import eu.motogymkhana.competition.model.Gender;
import eu.motogymkhana.competition.model.Rider;
import eu.motogymkhana.competition.rider.RiderManager;
import eu.motogymkhana.competition.rider.UpdateRiderCallback;
import roboguice.activity.RoboActivity;

public class RiderNewUpdateActivity extends RoboActivity {

    public static final String RIDER_NUMBER = "rider_number";
    public static final String FOCUS = "focus";

    @Inject
    private RiderManager riderManager;
    private int number = 99;

    Rider rider = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_rider_input);

        final EditText firstNameView = (EditText) findViewById(R.id.first_name);
        final EditText lastNameView = (EditText) findViewById(R.id.last_name);
        final EditText numberView = (EditText) findViewById(R.id.number);
        final Spinner countrySpinner = (Spinner) findViewById(R.id.country);
        final Spinner bibSpinner = (Spinner) findViewById(R.id.bib);
        final CheckBox genderButton = (CheckBox) findViewById(R.id.gender);
        numberView.setText(Integer.toString(number));
        final TextView errorText = (TextView) findViewById(R.id.error_text);
        final int riderNumber = getIntent().getIntExtra(RIDER_NUMBER, -1);
        final EditText sharingWithView = (EditText) findViewById(R.id.sharing_with);

        ArrayAdapter<CharSequence> countrySpinAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        countrySpinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        for (Country item : Country.values()) {
            countrySpinAdapter.add(item.name());
        }

        countrySpinner.setAdapter(countrySpinAdapter);

        ArrayAdapter<CharSequence> bibSpinAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        countrySpinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        for (Bib item : Bib.values()) {
            bibSpinAdapter.add(item.name());
        }

        bibSpinner.setAdapter(bibSpinAdapter);

        if (riderNumber >= 0) {

            try {
                rider = riderManager.getRider(riderNumber);

                firstNameView.setText(rider.getFirstName());
                lastNameView.setText(rider.getLastName());
                numberView.setText(rider.getRiderNumberString());
                genderButton.setChecked(rider.getGender() == Gender.F);
                sharingWithView.setText(Integer.toString(rider.getSharing()));

                if (rider.getCountry() == null) {
                    rider.setCountry(Country.NL);
                } else {
                    for (int i = 0; i < Country.values().length; i++) {
                        if (Country.values()[i] == rider.getCountry()) {
                            countrySpinner.setSelection(i);
                            break;
                        }
                    }
                }

                if (rider.getBib() == null) {
                    rider.setBib(Bib.Y);
                } else {
                    for (int i = 0; i < Bib.values().length; i++) {
                        if (Bib.values()[i] == rider.getBib()) {
                            bibSpinner.setSelection(i);
                            break;
                        }
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
                errorText.setText(e.getMessage());
            }

        } else {
            rider = new Rider();
        }

        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rider.setCountry(Country.values()[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bibSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rider.setBib(Bib.values()[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ((Button) findViewById(R.id.deleteButton)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                riderManager.deleteRider(rider, new RidersCallback() {

                    @Override
                    public void onSuccess() {
                        finish();
                    }

                    @Override
                    public void onError() {
                        errorText.setText("Delete failed....");
                    }
                });
            }
        });


        ((Button) findViewById(R.id.okButton)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                String firstName = firstNameView.getText().toString();
                String lastName = lastNameView.getText().toString();
                Gender gender = genderButton.isChecked() ? Gender.F : Gender.M;
                String numberString = numberView.getText().toString();
                String countryString = (String) countrySpinner.getSelectedItem();
                String bibString = (String) bibSpinner.getSelectedItem();

                String sharingText = sharingWithView.getText().toString();
                int sharing = 0;
                if(sharingText !=null && sharingText.length()>0) {
                     sharing = Integer.parseInt(sharingWithView.getText().toString());
                 }
                Country country = Country.NL;
                if (countryString != null) {
                    country = Country.valueOf(countryString);
                }

                Bib bib = Bib.Y;
                if (bibString != null) {
                    bib = Bib.valueOf(bibString);
                }

                if (numberString != null && StringUtils.isNumeric(numberString)) {
                    number = Integer.parseInt(numberString);
                }

                rider.setFirstName(firstName);
                rider.setLastName(lastName);
                rider.setGender(gender);
                rider.setRiderNumber(number);
                rider.setCountry(country);
                rider.setSharing(sharing);

                if (firstName != null && lastName != null) {

                    try {
                        riderManager.createOrUpdate(rider, new UpdateRiderCallback() {

                            @Override
                            public void onSuccess() {
                                finish();
                            }

                            @Override
                            public void onError(String error) {
                                errorText.setText(error);
                            }
                        });
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

    }
}
