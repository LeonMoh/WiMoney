package com.example.wimoney;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.blikoon.qrcodescanner.QrCodeActivity;
import com.example.wimoney.Amount;
import com.example.wimoney.BaseActivity;
import com.example.wimoney.MerchantLogin;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final int BALANCE_CHECK = 101;
    private static boolean permissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button balance = findViewById(R.id.emailEnter);
        balance.setOnClickListener(this);

        Button sale = findViewById(R.id.sale);
        sale.setOnClickListener(this);

        ImageButton settings = findViewById(R.id.settings);
        settings.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        checkPermission();
        super.onStart();
    }

    @Override
    public void onClick(View view) {
        if (!permissionGranted)
            return;

        if ((view.getId() != R.id.settings) && isTokenRequired()) {
            toast("Merchant login required");
            Intent settings = new Intent(this, MerchantLogin.class);
            startActivity(settings);
            finish();
            return;
        }

        switch (view.getId()) {
            case R.id.emailEnter:
                Intent i = new Intent(MainActivity.this, QrCodeActivity.class);
                startActivityForResult(i, BALANCE_CHECK);
                break;
            case R.id.sale:
                Intent i2 = new Intent(MainActivity.this, Amount.class);
                startActivity(i2);
                finish();
                break;
            case R.id.settings:
                Intent settings = new Intent(this, MerchantLogin.class);
                startActivity(settings);
                finish();
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            Timber.e("COULD NOT GET A GOOD RESULT.");
            Toast.makeText(this, "Could not get QR code result", Toast.LENGTH_SHORT).show();
            return;
        }
        if (requestCode == BALANCE_CHECK) {
            if (data == null)
                return;
            //Getting the passed result
            String voucher = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            Timber.e("Voucher number: " + voucher);

            checkBalanceRequest(voucher);

            RelativeLayout main = findViewById(R.id.main);
            showLoadingPopup(main);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    permissionGranted = false;
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else
            permissionGranted = true;
    }

    private void checkBalanceRequest(String voucher) {
        HashMap<String, String> params = new HashMap<>();
        params.put("voucher", voucher);
        params.put("amount", String.valueOf(0));

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.voucher_data), new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                closeLoadingPopup();
                Log.d("CheckBalance Response", response.toString());
                try {
                    JSONObject data = null;
                    data = response.getJSONObject("data");
                    JSONObject receiver = data.getJSONObject("receiver");
                    String current_value = data.getString("current_value");
                    showBalanceAlert(receiver.getString("fname") + " " + receiver.getString("lname"), Double.parseDouble(current_value));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                closeLoadingPopup();

//                String statusCode = String.valueOf(error.networkResponse.statusCode);
                String body;
                //get response body and parse with appropriate encoding
                if (error.networkResponse.data != null) {
                    try {
                        body = new String(error.networkResponse.data, "UTF-8");
                        JSONObject response = new JSONObject(body);
                        body = response.getString("message");
                        toast(body);
                        if(body.contains("Unauthenticated")){
                            Intent i = new Intent(getApplicationContext(), MerchantLogin.class);
                            startActivity(i);
                            finish();
                        }
                    } catch (UnsupportedEncodingException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getMerchantId());
                headers.put("Accept", "application/json");
                return headers;
            }
        };


        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, 0, 0));

        NetworkRequest.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }


    private void showBalanceAlert(String name, double balance) {
        View v = getLayoutInflater().inflate(R.layout.balance_check_layout, null);
        TextView n = v.findViewById(R.id.index);
        n.setText(getString(R.string.name, name));
        TextView b = v.findViewById(R.id.emailEnter);
        b.setText(getString(R.string.balance, balance));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v);

        AlertDialog dialog = builder.create();

        Button ok = v.findViewById(R.id.ok);
        ok.setOnClickListener(view -> {
            dialog.dismiss();
            showFullscreen();
        });

        dialog.show();
    }
}
