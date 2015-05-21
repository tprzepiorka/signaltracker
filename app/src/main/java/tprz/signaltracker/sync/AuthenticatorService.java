package tprz.signaltracker.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by tomprz on 21/05/2015.
 */
public class AuthenticatorService extends Service {

    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
