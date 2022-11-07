package com.example.wimoney;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AlertDialog;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class TransactionHistory extends BaseActivity {

    private static int pageNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);
        Log.e("ERRdOPEN", String.valueOf(pageNumber));
        Button back = findViewById(R.id.back);
        back.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        Button settlement = findViewById(R.id.settlement);
        settlement.setOnClickListener(view -> {
            showAlertConfirmation();
        });

        Button next = findViewById(R.id.next);
        next.setOnClickListener(view -> {
            pageNumber++;
            loadPendingSettlements();
        });

        Button previous = findViewById(R.id.previous);
        previous.setOnClickListener(view -> {
            if (pageNumber == 0)
                return;
            pageNumber--;
            loadPendingSettlements();
        });

        loadPendingSettlements();
    }

    private void loadPendingSettlements() {
        HashMap<String, String> params = new HashMap<>();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, getString(R.string.transaction_history), new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("data");
                    if (jsonArray.length() == 0)
                        toast("No Unsettled transactions found");
                    else
                        createSettlementList(jsonArray);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Timber.e(e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                String statusCode = String.valueOf(error.networkResponse.statusCode);
                String body;
                //get response body and parse with appropriate encoding
//                if (error.networkResponse.data != null) {
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
//                }
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

    private void getSettlementReport() {
        HashMap<String, String> params = new HashMap<>();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, getString(R.string.settlement_report), new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getString("status").equals("success")) {
                        JSONArray jsonArray = response.getJSONArray("transactions");
                        double total = response.getDouble("amount");
                        if (jsonArray.length() == 0)
                            toast("No transactions found");
                        else
                            printSettlement(jsonArray, total);
                    } else
                        toast(response.getString("msg"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Timber.e(e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                String statusCode = String.valueOf(error.networkResponse.statusCode);
                String body;
                //get response body and parse with appropriate encoding
                if (error.networkResponse.data != null) {
                    try {
                        body = new String(error.networkResponse.data, "UTF-8");
                        JSONObject response = new JSONObject(body);
                        body = response.getString("message");
                        if(body.contains("Unauthenticated")){
                            Intent i = new Intent(getApplicationContext(), MerchantLogin.class);
                            startActivity(i);
                            finish();
                        }else{
                            toast("No transactions to settle");
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



    private void showAlertConfirmation() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Print daily settlement report").setTitle("Print Settlement?");

        builder.setPositiveButton("Confirm", (dialog, id) -> {
            getSettlementReport();
            showFullscreen();
        });
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            dialog.dismiss();
            showFullscreen();
        });

        AlertDialog dialog = builder.create();

        dialog.show();

    }

    private void createSettlementList(JSONArray jsonArray) {
        LinearLayout linearLayout = findViewById(R.id.linear);
        linearLayout.removeAllViews();
        linearLayout.addView(createHeader());

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                linearLayout.addView(createRow(jsonArray.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private LinearLayout createHeader() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f));
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        linearLayout.setBackgroundColor(Color.DKGRAY);

        TextView trxnId = new TextView(this);
        trxnId.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        trxnId.setText("TID");
        trxnId.setTextColor(Color.WHITE);
        trxnId.setTextSize(16);

        TextView txnTotal = new TextView(this);
        txnTotal.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        txnTotal.setText("TOTAL");
        txnTotal.setTextColor(Color.WHITE);
        txnTotal.setTextSize(16);

        TextView txnDate = new TextView(this);
        txnDate.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        txnDate.setText("DATE");
        txnDate.setTextColor(Color.WHITE);
        txnDate.setTextSize(16);
        txnDate.setGravity(Gravity.CENTER_HORIZONTAL);


        linearLayout.addView(trxnId);
        linearLayout.addView(txnTotal);
        linearLayout.addView(txnDate);
//        linearLayout.addView(action);

        return linearLayout;
    }

    private LinearLayout createRow(JSONObject jsonObject) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f));
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setBackgroundColor(Color.GRAY);
        linearLayout.setPadding(5, 10, 5, 10);



        TextView trxnId = new TextView(this);
        trxnId.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        try {
            trxnId.setText(jsonObject.getString("transaction_id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        trxnId.setTextColor(Color.WHITE);
        trxnId.setTextSize(14);
        trxnId.setGravity(Gravity.LEFT);

        TextView txnTotal = new TextView(this);
        txnTotal.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        try {
            txnTotal.setText("$" + jsonObject.getString("amount"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        txnTotal.setTextColor(Color.WHITE);
        txnTotal.setTextSize(14);
        txnTotal.setGravity(Gravity.LEFT);
//        txnTotal.setPadding(140, 0, 0, 0);

        TextView txnDate = new TextView(this);
        txnDate.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        try {
            txnDate.setText(jsonObject.getString("created_at"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        txnDate.setTextColor(Color.WHITE);
        txnDate.setTextSize(14);
        txnDate.setGravity(Gravity.CENTER_HORIZONTAL);

        linearLayout.addView(trxnId);
        linearLayout.addView(txnTotal);
        linearLayout.addView(txnDate);
//        linearLayout.addView(view);

        return linearLayout;
    }

}
