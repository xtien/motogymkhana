package eu.motogymkhana.competition.rider;

import android.content.Context;

import com.google.inject.Inject;

import eu.motogymkhana.competition.dao.RiderDao;
import eu.motogymkhana.competition.dao.TimesDao;
import eu.motogymkhana.competition.round.RoundManager;
import roboguice.util.RoboAsyncTask;

/**
 * Created by christine on 17-5-15.
 */
public class StoreWittyRidersTask extends RoboAsyncTask<Void> {

    private Context context;

    @Inject
    private RiderDao riderDao;

    @Inject
    private RoundManager roundManager;

    @Inject
    private TimesDao timesDao;

    public StoreWittyRidersTask(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Void call() throws Exception {

//        InputStream is = context.getResources().openRawResource(R.raw.witty_row);
//        String row = IOUtils.toString(is);
//        IOUtils.closeQuietly(is);
//
//        is = context.getResources().openRawResource(R.raw.witty_header);
//        String s = IOUtils.toString(is);
//        IOUtils.closeQuietly(is);
//
//        File wittyFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/witty_riders.xml");
//        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(wittyFile));
//
//        bos.write(s.getBytes());
//
//        for (Rider rider : timesDao.getRegisteredRiders(roundManager.getDate())) {
//
//            String riderString = String.format(row, rider.getLastName(), rider.getFirstName(), rider.getGender().name
//                    (), rider.getRiderNumber(), rider.getStartNumber());
//            bos.write(riderString.getBytes());
//        }
//
//        is = context.getResources().openRawResource(R.raw.witty_footer);
//        String footer = IOUtils.toString(is);
//
//        IOUtils.closeQuietly(is);
//        bos.write(footer.getBytes());
//        bos.flush();
//        bos.close();
//
        return null;
    }

    @Override
    public void onException(Exception e) {
        e.printStackTrace();
    }
}
