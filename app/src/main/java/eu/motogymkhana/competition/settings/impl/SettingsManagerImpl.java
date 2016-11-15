package eu.motogymkhana.competition.settings.impl;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import eu.motogymkhana.competition.Constants;
import eu.motogymkhana.competition.api.ApiManager;
import eu.motogymkhana.competition.api.ResponseHandler;
import eu.motogymkhana.competition.dao.SettingsDao;
import eu.motogymkhana.competition.model.Country;
import eu.motogymkhana.competition.model.Round;
import eu.motogymkhana.competition.rider.RiderManager;
import eu.motogymkhana.competition.settings.Settings;
import eu.motogymkhana.competition.settings.SettingsManager;
import toothpick.Lazy;

/**
 * Created by christine on 21-2-16.
 */
@Singleton
public class SettingsManagerImpl implements SettingsManager {

    @Inject
    protected Context context;

    @Inject
    protected Lazy<RiderManager> riderManager;

    @Inject
    protected Lazy<ApiManager> apiManager;

    @Inject
    protected SettingsDao settingsDao;

    private ResponseHandler getSettingsFromServerResponseHandler = new ResponseHandler() {

        @Override
        public void onSuccess(Object object) {
            try {
                settingsDao.store((Settings) object);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onException(Exception e) {

        }

        @Override
        public void onError(int statusCode, String string) {

        }
    };

    private ResponseHandler uploadSettingsResponseHandler = new ResponseHandler() {

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

    @Override
    public void getSettingsFromServer(ResponseHandler responseHandler) {
        apiManager.get().getSettings(responseHandler);
     }

    @Override
    public void setSettings(Settings settings){

        if (settings == null) {
            settings = new Settings();
            settings.setCountry(Constants.country);
            settings.setSeason(Constants.season);
        }
        try {
            settingsDao.store(settings);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void store(Settings settings) throws SQLException {
        settingsDao.store(settings);
    }

    @Override
    public void storeCountryAndSeason(Country country, int season) throws SQLException {
        settingsDao.storeCountryAndSeason(country,season);
    }

    @Override
    public void uploadSettingsToServer(Settings settings, ResponseHandler responseHandler) {
        apiManager.get().uploadSettings(settings, responseHandler);
    }

    @Override
    public int getRoundsCountingForSeasonResult() {
        Settings settings = null;
        try {
            settings = settingsDao.get();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
        return settings != null ? settings.getRoundsForSeasonResult() : 0;
    }

    @Override
    public Settings getSettings() throws IOException, SQLException {

        Settings settings = null;

        try {
            settings = settingsDao.get();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (settings == null) {
            getSettingsFromServer(getSettingsFromServerResponseHandler);
            settings = new Settings();
            settings.setSeason(Constants.season);
            settings.setCountry(Constants.country);
            try {
                settingsDao.store(settings);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return settings;
    }

    @Override
    public void setRounds(List<Round> rounds) throws IOException, SQLException {

        Settings settings = getSettings();
        settings.setHasRounds(rounds != null && rounds.size() > 0);
        settingsDao.storeHasRounds(settings.hasRounds());
        uploadSettingsToServer(settings, uploadSettingsResponseHandler);
    }
}
