package com.example.wimoney;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.myloadingbutton.MyLoadingButton;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;
import studio.carbonylgroup.textfieldboxes.TextFieldBoxes;



public class MerchantLogin extends BaseActivity implements MyLoadingButton.MyLoadingButtonClick {

    private MyLoadingButton myLoadingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_login);
        Button back = findViewById(R.id.back);
        back.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        MyLoadingButton register = findViewById(R.id.register);
        register.setMyButtonClickListener(() -> {
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivity(intent);
        });

        EditText email = findViewById(R.id.email);
        email.setText(getEmail());

        myLoadingButton = findViewById(R.id.login_button);
        myLoadingButton.setMyButtonClickListener(this); // Set MyLoadingButton click listener
    }

    @Override
    public void onMyLoadingButtonClick() {
        boolean a = true, b = true;

        TextFieldBoxes emailTextFieldBoxes = findViewById(R.id.text_field_boxes);
        TextFieldBoxes passwordTextFieldBoxes2 = findViewById(R.id.text_field_boxes2);

        ExtendedEditText email = findViewById(R.id.email);
        String e = email.getText().toString();

        ExtendedEditText password = findViewById(R.id.password);
        String p = password.getText().toString();

        if (e.length() == 0) {
            emailTextFieldBoxes.setError("Email required", false);
            a = false;
        }
        if (p.length() == 0) {
            Log.d("password", "missing");
            passwordTextFieldBoxes2.setError("Password required", false);
            b = false;
        }

        if (a && b) {
            myLoadingButton.showLoadingButton();
            saveEmail(e);
            login(e, p);

        } else
            myLoadingButton.showNormalButton();
    }


    private void login(String email, String password) {
        HashMap<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.login), new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                closeLoadingPopup();
                Log.e("ErrdResponce", response.toString());
                try {
                    Hawk.put("access_token", response.getString("token"));
                    Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                    Log.e("ERRdHAWHRes", response.toString());

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("ERRdException", e.toString());

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                closeLoadingPopup();

                String statusCode = String.valueOf(error.networkResponse.statusCode);
                Log.e("ErrdStatCode", statusCode.toString());

                String body;
                //get response body and parse with appropriate encoding
                if (error.networkResponse.data != null) {
                    try {
                        body = new String(error.networkResponse.data, "UTF-8");
                        JSONObject response = new JSONObject(body);
                        body = response.getString("message");
                        Log.e("ERRdNetResponce", body.toString());

                        toast(body);
                    } catch (UnsupportedEncodingException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        NetworkRequest.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }


    private void saveEmail(String email) {
        Hawk.put("merchant_email", email);
    }

    private String getEmail() {
        return Hawk.get("merchant_email", "");
    }
}
