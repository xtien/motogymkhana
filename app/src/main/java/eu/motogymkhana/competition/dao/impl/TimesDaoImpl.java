package eu.motogymkhana.competition.dao.impl;

import com.google.inject.Singleton;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import eu.motogymkhana.competition.Constants;
import eu.motogymkhana.competition.dao.RiderDao;
import eu.motogymkhana.competition.dao.TimesDao;
import eu.motogymkhana.competition.db.GymkhanaDatabaseHelper;
import eu.motogymkhana.competition.model.Bib;
import eu.motogymkhana.competition.model.Country;
import eu.motogymkhana.competition.model.Rider;
import eu.motogymkhana.competition.model.Times;

@Singleton
public class TimesDaoImpl extends BaseDaoImpl<Times, Integer> implements TimesDao {

    private static final boolean ASCENDING = true;
    private static final String LOGTAG = TimesDaoImpl.class.getSimpleName();
    private static final boolean UNREGISTERED = false;
    private static final boolean REGISTERED = true;

    public TimesDaoImpl(ConnectionSource connectionSource, Class<Times> dataClass) throws SQLException {
        super(connectionSource, Times.class);
    }

    @Override
    public void storeRiderTimes(Rider rider) throws SQLException {

        List<Times> existingRiderTimes = getTimes(rider.get_id());

        Iterator<Times> iterator = rider.getTimes().iterator();

        while (iterator.hasNext()) {
            Times times = iterator.next();
            times.setRider(rider);
            store(times);
            if (existingRiderTimes.contains(times)) {
                existingRiderTimes.remove(times);
            }
        }

        for (Times times : existingRiderTimes) {
            delete(times);
        }
    }

    @Override
    public Times getTimes(int rider_id, long date) throws SQLException {

        List<Times> list = null;
        QueryBuilder<Times, Integer> statementBuilder = queryBuilder();

        statementBuilder.where()
                .eq(Times.RIDER, rider_id).and()
                .eq(Times.DATE, date).and()
                .eq(Times.COUNTRY, Constants.country).and()
                .eq(Times.SEASON, Constants.season);

        list = query(statementBuilder.prepare());

        if (list != null && list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    private List<Times> getTimes(int rider_id) throws SQLException {

        List<Times> list = null;
        QueryBuilder<Times, Integer> statementBuilder = queryBuilder();

        statementBuilder.where()
                .eq(Times.RIDER, rider_id).and()
                .eq(Times.COUNTRY, Constants.country).and()
                .eq(Times.SEASON, Constants.season);

        list = query(statementBuilder.prepare());
        return list;
    }

    @Override
    public List<Times> getTimes(long date) throws SQLException {

        QueryBuilder<Times, Integer> statementBuilder = queryBuilder();

        Where<Times, Integer> where = statementBuilder.where();
        where.eq(Times.DATE, date).and()
                .eq(Times.REGISTERED, true).and()
                .eq(Times.COUNTRY, Constants.country).and()
                .eq(Times.SEASON, Constants.season);

        List<Times> list = query(statementBuilder.prepare());

        return list;
    }

    @Override
    public List<Times> getTimesSortedOnStartNumber(long date, Bib bib) throws SQLException {

        RiderDao riderDao = GymkhanaDatabaseHelper.getInstance().getDao(Rider.class);
        QueryBuilder<Rider, Integer> riderQueryBuilder = riderDao.queryBuilder();
        Where<Rider, Integer> riderWhere = riderQueryBuilder.where();
        riderWhere.eq(Rider.BIB, bib);

        QueryBuilder<Times, Integer> statementBuilder = queryBuilder();
        Where<Times, Integer> where = statementBuilder.where();
        where.eq(Times.DATE, date).and()
                .eq(Times.REGISTERED, true).and()
                .eq(Times.COUNTRY, Constants.country).and()
                .eq(Times.SEASON, Constants.season);
        statementBuilder.orderBy(Times.START_NUMBER, ASCENDING);

        List<Times> result = statementBuilder.join(riderQueryBuilder).query();

        return result;
    }

    @Override
    public List<Rider> getRiders(long date) throws SQLException {

        QueryBuilder<Times, Integer> statementBuilder = queryBuilder();

        Where<Times, Integer> where = statementBuilder.where();
        where.eq(Times.DATE, date).and()
                .eq(Times.COUNTRY, Constants.country).and()
                .eq(Times.SEASON, Constants.season);

        List<Rider> riders = getRiders(query(statementBuilder.prepare()));

        return riders;
    }

    @Override
    public void store(Times times) throws SQLException {

        Times existingTimes = getTimes(times.getRider().get_id(), times.getDate());

        if (existingTimes == null) {

            create(times);

        } else {

            if (times.newerThan(existingTimes)) {
                times.set_id(existingTimes.get_id());
                times.setTimeStamp(System.currentTimeMillis());
                update(times);
            }
        }
    }

    @Override
    public void updateStartNumber(Times times) throws SQLException {

        Times existingTimes = getTimes(times.getRider().get_id(), times.getDate());

        if (existingTimes == null) {

            create(times);

        } else {
            existingTimes.setStartNumber(times.getStartNumber());
            update(existingTimes);
        }
    }

    @Override
    public int create(Times times) throws SQLException {
        //TODO just to be sure, set country and season if null
        return super.create(times);
    }

    @Override
    public void clear() throws SQLException {

        List<Rider> list = null;
        DeleteBuilder<Times, Integer> statementBuilder = deleteBuilder();

        statementBuilder.where()
                .eq(Times.COUNTRY, Constants.country).and()
                .eq(Times.SEASON, Constants.season);

        statementBuilder.delete();
    }

    @Override
    public List<Rider> getUnregisteredRiders(long date, Bib bib) throws SQLException {
        return getRiders(date, bib, UNREGISTERED);
    }

    @Override
    public List<Rider> getRegisteredRiders(long date, Bib bib) throws SQLException {
        return getRiders(date, bib, REGISTERED);
    }

    private List<Rider> getRiders(long date, Bib bib, boolean registered) throws SQLException {

        QueryBuilder<Times, Integer> timesQueryBuilder = queryBuilder();
        Where<Times, Integer> where = timesQueryBuilder.where();
        where.eq(Times.REGISTERED, registered).and()
                .eq(Times.DATE, date).and()
                .eq(Times.COUNTRY, Constants.country).and()
                .eq(Times.SEASON, Constants.season);

        RiderDao riderDao = GymkhanaDatabaseHelper.getInstance().getDao(Rider.class);
        QueryBuilder<Rider, Integer> riderQueryBuilder = riderDao.queryBuilder();

        Where<Rider, Integer> riderWhere = riderQueryBuilder.where();

        riderWhere.eq(Rider.BIB, bib);

        List<Rider> result = riderQueryBuilder.join(timesQueryBuilder).query();

        return result;
    }

    @Override
    public List<Rider> getRegisteredRiders(long date) throws SQLException {

        QueryBuilder<Times, Integer> statementBuilder = queryBuilder();

        statementBuilder.where()
                .eq(Times.REGISTERED, true).and()
                .eq(Times.DATE, date).and()
                .eq(Times.COUNTRY, Constants.country).and()
                .eq(Times.SEASON, Constants.season);

        return getRiders(query(statementBuilder.prepare()));
    }

    private List<Rider> getRiders(List<Times> timesList) {

        List<Rider> riders = new ArrayList<Rider>();

        for (Times times : timesList) {
            if (times.hasRider()) {
                riders.add(times.getRider());
            }
        }

        return riders;
    }


    @Override
    public void delete(Country country, int season) throws SQLException {

        DeleteBuilder<Times, Integer> statementBuilder = deleteBuilder();
        statementBuilder.where().eq(Times.COUNTRY, country).and().eq(Times.SEASON, season);
        statementBuilder.delete();
    }

}
