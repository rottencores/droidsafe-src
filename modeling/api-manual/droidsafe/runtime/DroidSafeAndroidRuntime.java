package droidsafe.runtime;

import java.util.Random;

import android.app.ContextImpl;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.app.Application;

import droidsafe.annotations.DSC;
import droidsafe.annotations.DSModeled;

/**
 * This class will simulate the android runtime system by making any calls or creating 
 * any globals required.
 * 
 * 
 * @author mgordon
 *
 */
public class DroidSafeAndroidRuntime {
    public static boolean control = new Random().nextBoolean();
    public static int switchControl = new Random().nextInt();
    private static Application mApplication;

    @DSModeled
    /**
     * This method will be called automatically by the droidsafe harness class before all
     * application code.
     * 	
     * @param args
     */
    public static void main() {

    }

    /**
     * create any associated state and call init methods on an activity
     * 
     * call any life cycle events for the activity
     * 
     * @param activity
     */
    @DSModeled(DSC.SPEC)
    public static void modelActivity(android.app.Activity activity) {
        ContextImpl context = new ContextImpl();

        if (mApplication != null)
            activity.setApplication(mApplication);

        Bundle b = new Bundle();
        //onsavedinstancestate(b)
        activity.performCreate(b, context);


        activity.droidsafeOnResume();
        activity.droidsafeOnStop();
        activity.droidsafeOnDestroy();

    }

    @DSModeled(DSC.SPEC)
    public static void modelService(android.app.Service service) {
        if (mApplication != null)
            service.setApplication(mApplication);

    }

    @DSModeled(DSC.SPEC)
    public static void modelContentProvider(android.content.ContentProvider contentProvider) {

    }

    @DSModeled(DSC.SPEC)
    public static void modelBroadCastReceiver(BroadcastReceiver receiver) {
        receiver.onReceive(new ContextImpl(), new Intent());
    }

    @DSModeled(DSC.SPEC)
    public static void modelApplication(android.app.Application app) {
        mApplication = app;

        app.droidsafeOnCreate();
        app.droidsafeOnTerminate();
        app.droidsafeOnEverythingElse();
    }
}