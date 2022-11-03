package com.example.wimoney;

import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.blikoon.qrcodescanner.QrCodeActivity;
import com.davidmiguel.numberkeyboard.NumberKeyboard;
import com.davidmiguel.numberkeyboard.NumberKeyboardListener;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.orhanobut.hawk.Hawk;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class Amount extends BaseActivity implements View.OnClickListener {
    private static final int SALE = 102;
    private StringBuilder stringBuilder;
    private double amt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amount);

        EditText amount = findViewById(R.id.amount);

        Button back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Amount.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });


        Button next = findViewById(R.id.next);
        next.setOnClickListener(view -> {
            amt = 0.0;
            try {
                amt = Double.parseDouble(stringBuilder.toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid Amount", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amt < 1) {
                Toast.makeText(this, "Amount cannot be less than $1.00", Toast.LENGTH_SHORT).show();
                amount.setError("error");
                amount.requestFocus();
                Log.e("ERRdAmount", amount.toString());

                return;
            }
            amount.setError(null);
            Hawk.put("amount", amt);
            showAlertConfirmation(stringBuilder.toString());
            Log.e("ERRdAmount2", amount.toString());
            Log.e("ERRdAmount22", stringBuilder.toString());


        });

        stringBuilder = new StringBuilder();


        NumberKeyboard keyboard = findViewById(R.id.keypad);
        keyboard.setListener(new NumberKeyboardListener() {
            @Override
            public void onNumberClicked(int i) {
                stringBuilder.append(i);
                amount.setText(stringBuilder.toString());
                Log.e("ERRdAmount3", amount.toString());

            }

            @Override
            public void onLeftAuxButtonClicked() {
                if (stringBuilder.toString().contains("."))
                    return;
                stringBuilder.append(".");
                amount.setText(stringBuilder.toString());
                Log.e("ERRdAmount4", amount.toString());

            }

            @Override
            public void onRightAuxButtonClicked() {
                if (stringBuilder.length() == 0)
                    return;
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                amount.setText(stringBuilder.toString());
                Log.e("ERRdAmount5", amount.toString());


            }
        });

    }

    private void showAlertConfirmation(String amt) {
        View v = getLayoutInflater().inflate(R.layout.perform_sale, null);

        TextView msg = v.findViewById(R.id.msg);
        msg.setText("You are about to perform a transaction for $" + amt + ".\n\nScan voucher OR enter code?");

        Log.e("ERRdMsg", msg.toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v);


        AlertDialog dialog = builder.create();

        Button cancel = v.findViewById(R.id.cancel);
        cancel.setOnClickListener(view -> {
            dialog.dismiss();
            showFullscreen();
        });

        Button scan = v.findViewById(R.id.scan);
        scan.setOnClickListener(view -> {
            dialog.dismiss();
            Intent i = new Intent(this, QrCodeActivity.class);
            i.putExtra("amount", amt);
            Log.e("ERRdAmtScan", amt.toString());
            startActivityForResult(i, SALE);
        });

        Button type = v.findViewById(R.id.type);
        type.setOnClickListener(view -> {
            dialog.dismiss();
            showFullscreen();
            showVoucherNumberAlert();
        });

        dialog.show();
    }

    private void showVoucherNumberAlert() {
        View v = getLayoutInflater().inflate(R.layout.voucher_number, null);

        EditText voucher = v.findViewById(R.id.voucher);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter voucher number");
        builder.setView(v);


        AlertDialog dialog = builder.create();

        Button cancel = v.findViewById(R.id.cancel);
        cancel.setOnClickListener(view -> {
            dialog.dismiss();
            showFullscreen();
        });

        Button confirm = v.findViewById(R.id.confirm);
        confirm.setOnClickListener(view -> {
            dialog.dismiss();
            String vnum = voucher.getText().toString().trim();
            if (vnum.length() != 12) {
                toast("Voucher number must be 12 characters");
                return;
            }
            RelativeLayout main = findViewById(R.id.main);
            showLoadingPopup(main);
            getVoucherData(vnum);
        });

        dialog.show();
    }

    private void getVoucherData(String voucher) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, getString(R.string.voucher_data, voucher), null, response -> {
            try {
//                Timber.e(response.toString());
                Log.e("ERRdResponse", response.toString());
                if (response.getString("status").equals("success")) {
                    closeLoadingPopup();
                    showVoucherData(response.getDouble("balance"), response.getString("photo_id"), response.getString("customer_name"), voucher);
                } else {
                    closeLoadingPopup();
                    toast(response.getString("msg"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                closeLoadingPopup();
            }
        }, error -> {
            error.printStackTrace();
            closeLoadingPopup();
        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy());

        NetworkRequest.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    private void showVoucherData(double balance, String image, String name, String voucher) {

        View v = getLayoutInflater().inflate(R.layout.voucher_data, null);

        byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        TextView n = v.findViewById(R.id.name);
        n.setText(getString(R.string.name, name));

        TextView b = v.findViewById(R.id.balance);
        b.setText(getString(R.string.balance, balance));

        CircularImageView circularImageView = v.findViewById(R.id.image);
        circularImageView.setImageBitmap(bitmap);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v);


        AlertDialog dialog = builder.create();

        Button cancel = v.findViewById(R.id.cancel);
        cancel.setOnClickListener(view -> {
            dialog.dismiss();
            showFullscreen();
        });

        Button confirm = v.findViewById(R.id.confirm);
        confirm.setOnClickListener(view -> {
            dialog.dismiss();
            RelativeLayout main = findViewById(R.id.main);
            showLoadingPopup(main);

            amt = Hawk.get("amount", 0.0);

            saleRequest(voucher, amt);
        });

        closeLoadingPopup();

        dialog.show();
    }

    private void saleRequest(String voucher, double amt) {
        HashMap<String, String> params = new HashMap<>();
        params.put("voucher", voucher);
        params.put("amount", Double.toString(this.amt));
        params.put("merchant_id", Integer.toString(getMerchantId()));

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.sale, getMerchantId()), new JSONObject(params), response -> {
            try {
                if (response.getString("status").equals("success")) {
                    printReceipt(voucher, response.getString("transaction_id"), amt, response.getDouble("voucher_balance"));
                    new Handler().postDelayed(this::closeLoadingPopup, 2500);
                } else {
                    closeLoadingPopup();
                    toast(response.getString("msg"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                closeLoadingPopup();
                toast(e.toString());
            }
        }, error -> {
            closeLoadingPopup();
            toast(error.toString());
        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, 0, 0));

        NetworkRequest.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }


    @Override
    public void onClick(View view) {
        
    }
}