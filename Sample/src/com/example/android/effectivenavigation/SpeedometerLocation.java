package com.example.android.effectivenavigation;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;



public class SpeedometerLocation extends Fragment implements  GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener, LocationListener{
	 // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    // Handles to UI widgets
    private TextView timer;
    private TextView speed;
    private ProgressBar mActivityIndicator;
    private TextView avgSpeed;
    private TextView speedLimit;
    private TextView distance;
    private Button start,pause,stop;
    private Chronometer chronometer;
    LinearLayout Layout;

long timeWhenStopped = 0;
public static Context c;
    // Handle to SharedPreferences for this app
    SharedPreferences mPrefs;

    // Handle to a SharedPreferences editor
    SharedPreferences.Editor mEditor;

    /*
     * Note if updates have been turned on. Starts out as "false"; is set to "true" in the
     * method handleRequestSuccess of LocationUpdateReceiver.
     *
     */
    boolean mUpdatesRequested = false;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreateView(inflater, container, savedInstanceState);
		 View rootView = inflater.inflate(R.layout.fragment_section_launchpad, container, false);

		 c = this.getActivity();

        // Get handles to the UI view objects
        timer = (TextView) rootView.findViewById(R.id.timer);
        speed = (TextView) rootView.findViewById(R.id.speed_value);
        //mActivityIndicator = (ProgressBar) findViewById(R.id.address_progress);
        avgSpeed = (TextView) rootView.findViewById(R.id.avg_speed);
        speedLimit = (TextView) rootView.findViewById(R.id.speed_limit);
        distance=(TextView)rootView.findViewById(R.id.distance_value);
        start=(Button)rootView.findViewById(R.id.start);
        pause=(Button)rootView.findViewById(R.id.pause);
        stop=(Button)rootView.findViewById(R.id.stop);
        Layout = (LinearLayout)rootView.findViewById(R.id.hiddenLayout);
        chronometer=(Chronometer)rootView.findViewById(R.id.timer);
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();
        chronometer.setFormat("%s");
       
        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        // Note that location updates are off until the user turns them on
        mUpdatesRequested = false;

        // Open Shared Preferences
        mPrefs = this.getActivity().getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        // Get an editor
        mEditor = mPrefs.edit();

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(SpeedometerLocation.c, this, this);
     
        if (servicesConnected()) {
         	
      
                     start.setOnClickListener(new View.OnClickListener() {
                       @Override
                        public void onClick(View view) {
                    	   start.setVisibility(View.GONE);
                    	   Layout.setVisibility(View.VISIBLE);
                    	   chronometer.setBase(SystemClock.elapsedRealtime());
                    	  
                    	   chronometer.start();
                    	   getLocation();  
                    	   startUpdates();
                    	                  
                    	                 	  
                    	  
                    	   
                         }
                     });
                     pause.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							String name=pause.getText().toString();
							if(name.equals("PAUSE"))
							{
								
								pause.setText("RESUME");
								pause.setTextColor(-1);
								pause.setBackgroundColor(Color.parseColor("#92CE01"));
								timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime(); 
								chronometer.stop();
							  stopUpdates();
								
								
							}
							else
							{
								pause.setText("PAUSE");
								pause.setBackgroundColor(Color.parseColor("#F38F05"));

								 chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
								  chronometer.start();
								  startUpdates();
								   
							}
							
							
						}
					});
                     stop.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							stopPeriodicUpdates();

						
	                    	   Layout.setVisibility(View.GONE);
	                    		start.setVisibility(View.VISIBLE);	   
							chronometer.setBase(SystemClock.elapsedRealtime());
							timeWhenStopped = 0;
							chronometer.stop();
							
							mPrefs.edit().clear().commit();
							speed.setText("0.0");
							distance.setText("00.00");
							avgSpeed.setText("0.0");
							stopUpdates();
							
							
							}
					});
        
        }
		return rootView;

	}
	

	@Override
	public void onPause() {
		//e current setting for updates
        mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, mUpdatesRequested);
        mEditor.commit();
   
		// TODO Auto-generated method stub
		super.onPause();
	}


	

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		  // If the app already has a setting for getting location updates, get it
        if (mPrefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)) {
            mUpdatesRequested = mPrefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
          
        // Otherwise, turn off location updates until requested
        }
        else {
            mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
            mEditor.commit();
        }
        
        
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		  /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();
      
	}

	@Override
	public void onStop() {
		
		 // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }
        		

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();

        super.onStop();
	}


	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, intent);
		  // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.resolved));

                        // Display the result
                        //mConnectionState.setText(R.string.connected);
                        //mConnectionStatus.setText(R.string.resolved);
                    break;

                    // If any other result was returned by Google Play services
                    default:
                    	
                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));

                        // Display the result
                       // mConnectionState.setText(R.string.disconnected);
                        //mConnectionStatus.setText(R.string.no_resolution);

                    break;
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(LocationUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));

               break;
        }
	}
	 /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getActivity());

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this.getActivity(), 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(this.getActivity().getSupportFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }
    /**
     * Invoked by the "Get Location" button.
     *
     * Calls getLastLocation() to get the current location
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void getLocation() {

        // If Google Play Services is available
        if (servicesConnected()) {

            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();
                 String latLng =LocationUtils.getLatLng(this.getActivity(), currentLocation);
                 System.out.println("latlng="+latLng);
                 
                 mEditor.putString("latitude", ""+currentLocation.getLatitude());
                 mEditor.putString("longitude", ""+currentLocation.getLongitude());
                 mEditor.putString("distance",""+0);
                 
                 mEditor.commit();
            // Display the current location in the UI
           // mLatLng.setText(latLng);
        }
    }
    /**
     * Invoked by the "Get Address" button.
     * Get the address of the current location, using reverse geocoding. This only works if
     * a geocoding service is available.
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    // For Eclipse with ADT, suppress warnings about Geocoder.isPresent()
    @SuppressLint("NewApi")
    public void getAddress(View v) {

        // In Gingerbread and later, use Geocoder.isPresent() to see if a geocoder is available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && !Geocoder.isPresent()) {
            // No geocoder is present. Issue an error message
            Toast.makeText(this.getActivity(), R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
            return;
        }

        if (servicesConnected()) {

            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();

            // Turn the indefinite activity indicator on
            mActivityIndicator.setVisibility(View.VISIBLE);

            // Start the background task
            (new SpeedometerLocation.GetAddressTask(this.getActivity())).execute(currentLocation);
       
        }
    }
        /**
         * Invoked by the "Start Updates" button
         * Sends a request to start location updates
         *
         * @param v The view object associated with this method, in this case a Button.
         */ 
        public void startUpdates() {
            mUpdatesRequested = true;

            if (servicesConnected()) {
                startPeriodicUpdates();
            }
        }

        /**
         * Invoked by the "Stop Updates" button
         * Sends a request to remove location updates
         * request them.
         *
         * @param v The view object associated with this method, in this case a Button.
         */
        public void stopUpdates() {
            mUpdatesRequested = false;

            if (servicesConnected()) {
                stopPeriodicUpdates();
            }
        }

        /*
         * Called by Location Services when the request to connect the
         * client finishes successfully. At this point, you can
         * request the current location or start periodic updates
         */
        @Override
        public void onConnected(Bundle bundle) {
            //mConnectionStatus.setText(R.string.connected);

            if (mUpdatesRequested) {
                startPeriodicUpdates();
            }
        }

        /*
         * Called by Location Services if the connection to the
         * location client drops because of an error.
         */
        @Override
        public void onDisconnected() {
            //mConnectionStatus.setText(R.string.disconnected);
        }

        /*
         * Called by Location Services if the attempt to
         * Location Services fails.
         */
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

            /*
             * Google Play services can resolve some errors it detects.
             * If the error has a resolution, try sending an Intent to
             * start a Google Play services activity that can resolve
             * error.
             */
            if (connectionResult.hasResolution()) {
                try {
                 
                    // Start an Activity that tries to resolve the error
                    connectionResult.startResolutionForResult(
                            this.getActivity(),
                            LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                    /*
                    * Thrown if Google Play services canceled the original
                    * PendingIntent
                    */

                } catch (IntentSender.SendIntentException e) {

                    // Log the error
                    e.printStackTrace();
                }
            } else {

                // If no resolution is available, display a dialog to the user with the error.
                showErrorDialog(connectionResult.getErrorCode());
            }
        }

        /**
         * Report location updates to the UI.
         *
         * @param location The updated location.
         */
        @Override
        public void onLocationChanged(Location location) {

            // Report to the UI that the location was updated
            //mConnectionStatus.setText(R.string.location_updated);

            // In the UI, set the latitude and longitude to the value received
       

        	

           float temp;
           double startLatitude=Double.parseDouble(mPrefs.getString("latitude", "0"));
           double startLongitude=Double.parseDouble(mPrefs.getString("longitude", "0"));
           double length=Double.parseDouble(mPrefs.getString("distance", "0"));
           float[] results = new float[3];
            
          Location.distanceBetween(startLatitude, startLongitude,location.getLatitude(), location.getLongitude(),results);
          length +=results[0];
          if(location.hasSpeed())
          temp=location.getSpeed();
          else
        	  temp=0;
          distance.setText(""+Double.parseDouble(new DecimalFormat("##.##").format(length*0.001)));
          System.out.println("hello");
          long timeElapsed=SystemClock.elapsedRealtime()-chronometer.getBase();
          int hours = (int) (timeElapsed / 3600000);
          int minutes = (int) (timeElapsed - hours * 3600000) / 60000;
          int seconds=(int) (timeElapsed - hours * 3600000 - minutes * 60000) / 1000;
          double avgspeed=0;
          if(seconds>0&&temp>1)
          {
          avgspeed=length/seconds;
          }
          mEditor.putString("latitude", ""+location.getLatitude());
          mEditor.putString("longitude", ""+location.getLongitude());
          mEditor.putString("distance",""+length);
          mEditor.apply();
        
        	  
          speedLimit.setText(""+35); 
          
          speed.setText(""+Double.parseDouble(new DecimalFormat("##.#").format(temp*3.6)));
          avgSpeed.setText(""+Double.parseDouble(new DecimalFormat("##.#").format(avgspeed*3.6)));
            
            if((temp*3.6)>20)
            {
            	Vibrator v = (Vibrator)this.getActivity(). getSystemService(Context.VIBRATOR_SERVICE);
            	

            	// Vibrate for 400 milliseconds
            	v.vibrate(400);
            	if((temp*3.6)>35)
            	{
            		v.vibrate(600);
            	}
            }
          }
        
        
        
        /**
         * In response to a request to start updates, send a request
         * to Location Services
         */
        private void startPeriodicUpdates() {

            mLocationClient.requestLocationUpdates(mLocationRequest, this);
           // mConnectionState.setText(R.string.location_requested);
        }

        /**
         * In response to a request to stop updates, send a request to
         * Location Services
         */
        private void stopPeriodicUpdates() {
            mLocationClient.removeLocationUpdates(this);
            //mConnectionState.setText(R.string.location_updates_stopped);
        }

        /**
         * An AsyncTask that calls getFromLocation() in the background.
         * The class uses the following generic types:
         * Location - A {@link android.location.Location} object containing the current location,
         *            passed as the input parameter to doInBackground()
         * Void     - indicates that progress units are not used by this subclass
         * String   - An address passed to onPostExecute()
         */
        protected class GetAddressTask extends AsyncTask<Location, Void, String> {

            // Store the context passed to the AsyncTask when the system instantiates it.
            Context localContext;

            // Constructor called by the system to instantiate the task
            public GetAddressTask(Context context) {

                // Required by the semantics of AsyncTask
                super();

                // Set a Context for the background task
                localContext = context;
            }

            /**
             * Get a geocoding service instance, pass latitude and longitude to it, format the returned
             * address, and return the address to the UI thread.
             */
            @Override
            protected String doInBackground(Location... params) {
                /*
                 * Get a new geocoding service instance, set for localized addresses. This example uses
                 * android.location.Geocoder, but other geocoders that conform to address standards
                 * can also be used.
                 */
                Geocoder geocoder = new Geocoder(localContext, Locale.getDefault());

                // Get the current location from the input parameter list
                Location location = params[0];

                // Create a list to contain the result address
                List <Address> addresses = null;

                // Try to get an address for the current location. Catch IO or network problems.
                try {

                    /*
                     * Call the synchronous getFromLocation() method with the latitude and
                     * longitude of the current location. Return at most 1 address.
                     */
                    addresses = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1
                    );

                    // Catch network or other I/O problems.
                    } catch (IOException exception1) {

                        // Log an error and return an error message
                        Log.e(LocationUtils.APPTAG, getString(R.string.IO_Exception_getFromLocation));

                        // print the stack trace
                        exception1.printStackTrace();

                        // Return an error message
                        return "IO_Exception_getFromLocation";

                    // Catch incorrect latitude or longitude values
                    } catch (IllegalArgumentException exception2) {

                        // Construct a message containing the invalid arguments
                        String errorString = getString(
                                R.string.illegal_argument_exception,
                                location.getLatitude(),
                                location.getLongitude()
                        );
                        // Log the error and print the stack trace
                        Log.e(LocationUtils.APPTAG, errorString);
                        exception2.printStackTrace();

                        //
                        return errorString;
                    }
                    // If the reverse geocode returned an address
                    if (addresses != null && addresses.size() > 0) {

                        // Get the first address
                        Address address = addresses.get(0);

                        // Format the first line of address
                        String addressText = getString(R.string.address_output_string,

                                // If there's a street address, add it
                                address.getMaxAddressLineIndex() > 0 ?
                                        address.getAddressLine(0) : "",

                                // Locality is usually a city
                                address.getLocality(),

                                // The country of the address
                                address.getCountryName()
                        );

                        // Return the text
                        return addressText;

                    // If there aren't any addresses, post a message
                    } else {
                      return getString(R.string.no_address_found);
                    }
            }

            /**
             * A method that's called once doInBackground() completes. Set the text of the
             * UI element that displays the address. This method runs on the UI thread.
             */
            @Override
            protected void onPostExecute(String address) {

                // Turn off the progress bar
                mActivityIndicator.setVisibility(View.GONE);

                // Set the address in the UI
                //mAddress.setText(address);
            }
        }

        /**
         * Show a dialog returned by Google Play services for the
         * connection error code
         *
         * @param errorCode An error code returned from onConnectionFailed
         */
        private void showErrorDialog(int errorCode) {

            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this.getActivity(),
                LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {

                // Create a new DialogFragment in which to show the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();

                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);

                // Show the error dialog in the DialogFragment
               errorFragment.show(this.getActivity().getSupportFragmentManager(), LocationUtils.APPTAG);
            }
        }

        /**
         * Define a DialogFragment to display the error dialog generated in
         * showErrorDialog.
         */
        public static class ErrorDialogFragment extends DialogFragment {

            // Global field to contain the error dialog
            private Dialog mDialog;

            /**
             * Default constructor. Sets the dialog field to null
             */
            public ErrorDialogFragment() {
                super();
                mDialog = null;
            }

            /**
             * Set the dialog to display
             *
             * @param dialog An error dialog
             */
            public void setDialog(Dialog dialog) {
                mDialog = dialog;
            }

            /*
             * This method must return a Dialog to the DialogFragment.
             */
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                return mDialog;
            }
        }

		@Override
		public void onDestroy() {
			// TODO Auto-generated method stub
			super.onDestroy();
		}


		


		
	
	

}
