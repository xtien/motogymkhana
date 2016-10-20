/*
 * Copyright (c) 2015 - 2016, Christine Karman
 * This project is free software: you can redistribute it and/or modify it under the terms of
 * the Apache License, Version 2.0. You can find a copy of the license at
 * http://www. apache.org/licenses/LICENSE-2.0.
 */

package eu.motogymkhana.competition.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import eu.motogymkhana.competition.BuildConfig;
import eu.motogymkhana.competition.Constants;
import eu.motogymkhana.competition.R;
import eu.motogymkhana.competition.adapter.ChangeListener;
import eu.motogymkhana.competition.adapter.MyViewPagerAdapter;
import eu.motogymkhana.competition.api.ResponseHandler;
import eu.motogymkhana.competition.api.response.SettingsResult;
import eu.motogymkhana.competition.context.ContextProvider;
import eu.motogymkhana.competition.dao.CredentialDao;
import eu.motogymkhana.competition.dao.RiderDao;
import eu.motogymkhana.competition.dao.RoundDao;
import eu.motogymkhana.competition.dao.SettingsDao;
import eu.motogymkhana.competition.fragment.RiderRegistrationFragment;
import eu.motogymkhana.competition.fragment.RiderTimeInputFragment;
import eu.motogymkhana.competition.fragment.RidersResultFragment;
import eu.motogymkhana.competition.fragment.SeasonTotalsFragment;
import eu.motogymkhana.competition.log.MyLog;
import eu.motogymkhana.competition.model.Country;
import eu.motogymkhana.competition.model.Round;
import eu.motogymkhana.competition.notify.Notifier;
import eu.motogymkhana.competition.prefs.MyPreferences;
import eu.motogymkhana.competition.rider.RiderManager;
import eu.motogymkhana.competition.round.RoundManager;
import eu.motogymkhana.competition.settings.SettingsManager;
import eu.motogymkhana.competition.util.CopyDB;
import roboguice.RoboGuice;

/**
 * Created by christine on 7-2-16.
 * MainActivity contains a ViewPager that shows three fragments (four in admin mode) for registration (admin mode only),
 * start list, day results, season total results.
 * The menu shows options for "settings" and "admin". In admin mode, it shows additional options. In Settings, user can
 * select country/region and season.
 */

public class MainActivity extends BaseActivity {

    private static final int NEW_RIDER = 101;
    private static final int ADMIN = 102;
    private static final int TEXT = 103;
    private static final int ADMIN_SETTINGS = 104;
    private static final int SETTINGS = 105;
    public static final int RIDERTIMES = 106;

    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1001;

    private static final String LOGTAG = MainActivity.class.getSimpleName();

    private Menu menu;

    @SuppressWarnings("unused")
    @Inject
    private ContextProvider contextProvider;

    private ViewPager viewPager;

    private static Handler handler;

    @Inject
    private RiderManager riderManager;

    @Inject
    private RiderDao riderDao;

    @Inject
    private RoundDao roundDao;

    @Inject
    private Notifier notifier;

    private MenuItem menuItem;

    @Inject
    private RoundManager roundManager;

    @Inject
    private SettingsManager settingsManager;

    @Inject
    private CredentialDao credentialDao;

    @Inject
    private MyPreferences prefs;

    @Inject
    private MyLog log;

    private TextView dateView;
    private TextView messageTextView;
    private ProgressBar progressBar;
    private List<Fragment> fragments;

    private Runnable loadRoundsTask = new Runnable() {

        @Override
        public void run() {
            roundManager.loadRoundsFromServer();
            settingsManager.getSettingsFromServer(getSettingsResponseHandler);
        }
    };

    private Runnable refreshTask = new Runnable() {

        @Override
        public void run() {

            if (prefs.loadRounds()) {
                roundManager.loadRoundsFromServer();
                settingsManager.getSettingsFromServer(null);
            } else {
                riderManager.loadRidersFromServer(downloadRidersResponseHandler);
            }

            handler.postDelayed(this, Constants.refreshRate);
        }
    };

    private ChangeListener dataChangedListener = new ChangeListener() {

        @Override
        public void notifyDataChanged() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    setDate();
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    };

    private ResponseHandler getSettingsResponseHandler = new ResponseHandler() {

        @Override
        public void onSuccess(Object object) {

            SettingsResult result = (SettingsResult) object;
            settingsManager.setSettings(result.getSettings());
        }

        @Override
        public void onException(Exception e) {

        }

        @Override
        public void onError(int statusCode, String string) {

        }
    };

    private ResponseHandler startNumbersResponseHandler = new ResponseHandler() {

        @Override
        public void onSuccess(Object object) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifier.notifyDataChanged();
                }
            });
        }

        @Override
        public void onException(Exception e) {
            showAlert(e);
        }

        @Override
        public void onError(int statusCode, String string) {
            showAlert(statusCode, string);
        }
    };

    private ResponseHandler uploadRidersResponseHandler = new ResponseHandler() {

        @Override
        public void onSuccess(Object object) {

        }

        @Override
        public void onException(Exception e) {

        }

        @Override
        public void onError(int statusCode, String string) {

        }
    };

    private Runnable notifyDataChangedRunner = new Runnable() {

        @Override
        public void run() {
            notifier.notifyDataChanged();
        }
    };

    private ResponseHandler downloadRidersResponseHandler = new ResponseHandler() {

        @Override
        public void onSuccess(Object object) {
            runOnUiThread(notifyDataChangedRunner);
        }

        @Override
        public void onException(Exception e) {
            showAlert(e);
        }

        @Override
        public void onError(int statusCode, String string) {
            showAlert(statusCode, string);
        }
    };

    private ResponseHandler uploadRoundsResponseHandler = new ResponseHandler() {
        @Override
        public void onSuccess(Object object) {

        }

        @Override
        public void onException(Exception e) {
            showAlert(e);
        }

        @Override
        public void onError(int statusCode, String string) {
            showAlert(statusCode, string);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (menuItem != null) {
            menuItem.setTitle(Constants.country.name() + " " + Constants.season);
        }

        switch (requestCode) {

            case ADMIN:
                if (isAdmin()) {
                    setFragments();
                    setDate();
                }

                break;

            case SETTINGS:

                dateView.setText(Constants.dateFormat.format(prefs.getDate()));
                notifier.notifyDataChanged();

                if (data.getBooleanExtra(SettingsActivity.COUNTRY_CHANGED, false) || data.getBooleanExtra
                        (SettingsActivity.SEASON_CHANGED, false)) {

                    showProgressBar();
                    handler.post(loadRoundsTask);
                }

                break;

            case ADMIN_SETTINGS:
                break;

            default:

                for (Fragment fragment : fragments) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }

                break;
        }
    }

    private void showProgressBar() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {

            int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {

                try {


                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        File uidStoreFile = new File(Environment.getExternalStorageDirectory(), "uid.txt");

                        CopyDB.copyDB(getApplicationContext(), Constants.DATABASE_NAME, "motogymkhana/");
                    }

                } catch (Exception e) {
                    log.e(LOGTAG, e);
                }

            } else {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            showAlert(getResources().getString(R.string.storage_permission_text));
                        }
                    });

                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                }
            }
        }

        setContentView(R.layout.activity_main);
        RoboGuice.getInjector(getApplicationContext()).injectMembers(this);

        Constants.country = prefs.getCountry();
        Constants.season = prefs.getSeason();

        getActionBar().setTitle("");

        dateView = (TextView) findViewById(R.id.date);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(3);

        messageTextView = ((TextView) findViewById(R.id.message_text));
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        try {

            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;

            if (prefs.getVersionCode() != versionCode) {
                setAdmin(false);
                prefs.setVersionCode(versionCode);
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            setAdmin(false);
        }

        if (prefs.isFirstRun()) {
            Locale currentLocale = getResources().getConfiguration().locale;
            for (Country country : Country.values()) {
                if (currentLocale.getCountry().equalsIgnoreCase(country.toString())) {
                    Constants.country = country;
                }
            }
            Calendar calendar = Calendar.getInstance();
            Constants.season = calendar.get(Calendar.YEAR);

            try {
                settingsManager.storeCountryAndSeason(Constants.country, Constants.season);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Collection<Round> rounds = null;

        try {
            rounds = roundManager.getRounds();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        try {
            roundManager.loadRoundsFromServer();
            settingsManager.getSettingsFromServer(getSettingsResponseHandler);
            riderManager.downloadRiders(downloadRidersResponseHandler);

            if (rounds == null || rounds.size() == 0) {

                Round round = roundManager.getNextRound();
                if (round != null) {
                    roundManager.setDate(round.getDate());
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        handler = new Handler();

        setFragments();

        setDate();

        if (prefs.startUpTimes() > 0) {

            handler.postDelayed(new Runnable() {

                @Override
                public void run() {

                    viewPager.setCurrentItem(1);

                    handler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            viewPager.setCurrentItem(0);
                        }
                    }, 2000l);
                }
            }, 2000l);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RoboGuice.destroyInjector(this);

        handler.removeCallbacksAndMessages(null);
        handler = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (riderManager != null) {
            notifier.registerRiderResultListener(dataChangedListener);
        }

        if (!isAdmin()) {
            handler.post(refreshTask);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);

        if (notifier != null) {
            notifier.unRegisterRiderResultListener(dataChangedListener);
        }
    }

    private void setFragments() {
        fragments = new ArrayList<Fragment>();

        if (isAdmin()) {
            fragments.add(new RiderRegistrationFragment());
        }

        fragments.add(new RiderTimeInputFragment());
        fragments.add(new RidersResultFragment());
        fragments.add(new SeasonTotalsFragment());

        viewPager.setAdapter(new MyViewPagerAdapter(getSupportFragmentManager(), fragments));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {

            case MY_PERMISSIONS_REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    CopyDB.copyDB(getApplicationContext(), Constants.DATABASE_NAME, "motogymkhana/");
                }
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean admin = isAdmin();

        menu.findItem(R.id.load_file).setVisible(false);
        menu.findItem(R.id.download_riders).setVisible(admin);
        menu.findItem(R.id.upload_riders).setVisible(false);
        menu.findItem(R.id.upload_dates).setVisible(admin);
        menu.findItem(R.id.new_rider).setVisible(admin);
        menu.findItem(R.id.settings).setVisible(true);
        menu.findItem(R.id.start_numbers).setVisible(admin);
        menu.findItem(R.id.text).setVisible(admin);
        menu.findItem(R.id.admin_settings).setVisible(admin);
        menu.findItem(R.id.save_witty_file).setVisible(false);

        menuItem = menu.findItem(R.id.country);
        menuItem.setTitle(Constants.country.name() + " " + Constants.season);

        menu.findItem(R.id.admin).setVisible(!admin);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        this.menu = menu;

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {

            case R.id.admin_settings:
                startActivityForResult(new Intent(getApplicationContext(), AdminSettingsActivity.class), ADMIN_SETTINGS);
                return true;

            case R.id.settings:
                startActivityForResult(new Intent(getApplicationContext(), SettingsActivity.class), SETTINGS);
                return true;

            case R.id.start_numbers:
                riderManager.generateStartNumbers(startNumbersResponseHandler);
                return true;

            case R.id.new_rider:
                startActivityForResult(new Intent(getApplicationContext(), RiderNewUpdateActivity.class), NEW_RIDER);
                return true;

            case R.id.load_file:
                try {
                    riderManager.loadRidersFile();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return true;

            case R.id.download_riders:
                try {
                    riderManager.downloadRiders(downloadRidersResponseHandler);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;

            case R.id.upload_riders:

                try {
                    riderManager.uploadRiders(uploadRidersResponseHandler);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return true;

            case R.id.upload_dates:

                try {
                    roundManager.uploadRounds(uploadRoundsResponseHandler);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return true;

            case R.id.save_witty_file:
                riderManager.saveWittyFile();
                return true;

            case R.id.admin:

                startActivityForResult(new Intent(getApplicationContext(), AdminActivity.class), ADMIN);
                return true;

            case R.id.text:

                startActivityForResult(new Intent(getApplicationContext(), TextActivity.class), TEXT);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setDate() {

        long date = prefs.getDate();

        if (date == 0l) {
            try {
                Round round = roundDao.getCurrentRound();
                if (round != null) {
                    prefs.setDate(round.getDate());
                    date = round.getDate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (date != 0l) {
            dateView.setText(Constants.dateFormat.format(date));
        } else {
            dateView.setText(getResources().getString(Constants.season < 2016 ? R.string.no_rounds_held : R.string
                    .no_rounds_planned));
        }
    }

    public boolean isAdmin() {
        return credentialDao.isAdmin();
    }

    private void setAdmin(boolean b) {
        credentialDao.setAdmin(b);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
