package com.example.wimoney;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.davidmiguel.numberkeyboard.NumberKeyboard;
import com.davidmiguel.numberkeyboard.NumberKeyboardListener;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

public class Amount extends BaseActivity implements View.OnClickListener {

    private StringBuilder stringBuilder;
    private static final int SALE = 102;
    private static final int REQUEST_IMAGE_CAPTURE = 99;
    private double amt;
    private String user_id;
    private String user_email;
    private String user_phone;
    private String user_index;
    private String user_image;
    private String user_voucher;
    private Boolean enable_email = true;
    private Boolean enable_phone = true;
    private String currentPhotoPath;
    String base64Image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amount);

        EditText amount = findViewById(R.id.amount);

        Button back = findViewById(R.id.back);
        back.setOnClickListener(this);

        Button next = findViewById(R.id.next);
        next.setOnClickListener(view -> {
            amt = 0.0;
            try {
                amt = Double.parseDouble(stringBuilder.toString());
            } catch (NumberFormatException e) {
                toast("Invalid amount");
                return;
            }
            if (amt < 0.01) {
                toast("Amount cannot be less than $0.01");
                amount.setError("error");
                amount.requestFocus();
                return;
            }
            amount.setError(null);
            Hawk.put("amount", amt);
            showAlertConfirmation(stringBuilder.toString());
//            Toast.makeText(this, "Amount Entered: ".concat(stringBuilder.toString()), Toast.LENGTH_SHORT).show();

        });

        stringBuilder = new StringBuilder();

        NumberKeyboard keyboard = findViewById(R.id.keypad);
        keyboard.setListener(new NumberKeyboardListener() {
            @Override
            public void onNumberClicked(int i) {
                stringBuilder.append(i);
                amount.setText(stringBuilder.toString());
            }

            @Override
            public void onLeftAuxButtonClicked() {
                if (stringBuilder.toString().contains("."))
                    return;
                stringBuilder.append(".");
                amount.setText(stringBuilder.toString());
            }

            @Override
            public void onRightAuxButtonClicked() {
                if (stringBuilder.length() == 0)
                    return;
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                amount.setText(stringBuilder.toString());
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                Intent backIntent = new Intent(this, MainActivity.class);
                startActivity(backIntent);
                finish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, "Could not get QR code result", Toast.LENGTH_SHORT).show();
            return;
        }
        if (requestCode == SALE) {
            if (data == null)
                return;
            //Getting the passed result
            user_voucher = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");


            RelativeLayout main = findViewById(R.id.main);
            showLoadingPopup(main);
            getVoucherData();
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Bundle extras = data.getExtras();
            base64Image = encodeImage(currentPhotoPath);

            Bitmap imageBitmap = (Bitmap) extras.get("data");
//                 Log.d("filePath", String.valueOf(filePath));
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), picUri);
            showVoucherData(imageBitmap, user_index, user_email, user_phone);

        }

    }

//    private void store_image() {
//        HashMap<String, String> params = new HashMap<>();
//        params.put("receipt_image", base64Image);
//
//        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.sale, getMerchantId()), new JSONObject(params), response -> {
//            try {
//                if (response.getString("status").equals("success")) {
//                    new Handler().postDelayed(this::closeLoadingPopup, 2500);
//                } else {
//                    closeLoadingPopup();
//                    toast(response.getString("msg"));
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//                closeLoadingPopup();
//                toast(e.toString());
//            }
//        }, error -> {
//            closeLoadingPopup();
//            toast(error.toString());
//        });
//
//        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, 0, 0));
//
//        NetworkRequest.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
//    }

    private String encodeImage(String path) {
        File imagefile = new File(path);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(imagefile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap bm = BitmapFactory.decodeStream(fis);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);
        //Base64.de
        return encImage;

    }

    private void showAlertConfirmation(String amt) {
//        Toast.makeText(this, "Amount Entered: ".concat(stringBuilder.toString()), Toast.LENGTH_SHORT).show();
Log.e("ErrdShowConfirmationAMT", amt);
        View v = getLayoutInflater().inflate(R.layout.perform_sale, null);

        TextView msg = v.findViewById(R.id.msg);
        msg.setText("You are about to perform a transaction for $" + amt + ".\n\nScan voucher OR enter code?");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v);

//        builder.setMessage("You are about to perform a transaction for $" + amt + ".\n\nProceed to scanning voucher QR code?").setTitle("Confirm Amount");

//        builder.setPositiveButton("Confirm", (dialog, id) -> {
//            Intent i = new Intent(this, QrCodeActivity.class);
//            i.putExtra("amount", amt);
//            startActivityForResult(i, SALE);
//        });
//        builder.setNegativeButton("Cancel", (dialog, id) -> {
//            dialog.dismiss();
//            showFullscreen();
//        });

        AlertDialog dialog = builder.create();

        Button cancel = v.findViewById(R.id.cancel);
        cancel.setOnClickListener(view -> {
            dialog.dismiss();
            showFullscreen();
        });

        Button scan = v.findViewById(R.id.scan);
        scan.setOnClickListener(view -> {
            dialog.dismiss();
            Intent i = new Intent(Amount.this, QrCodeActivity.class);
            i.putExtra("amount", amt);
            Log.e("ERRdAMT", amt);
            Log.e("ERRdAMTQR", String.valueOf(SALE));
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

//        builder.setMessage("You are about to perform a transaction for $" + amt + ".\n\nProceed to scanning voucher QR code?").setTitle("Confirm Amount");

//        builder.setPositiveButton("Confirm", (dialog, id) -> {
//            Intent i = new Intent(this, QrCodeActivity.class);
//            i.putExtra("amount", amt);
//            startActivityForResult(i, SALE);
//        });
//        builder.setNegativeButton("Cancel", (dialog, id) -> {
//            dialog.dismiss();
//            showFullscreen();
//        });

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
            user_voucher = vnum;
            RelativeLayout main = findViewById(R.id.main);
            showLoadingPopup(main);
            getVoucherData();
        });

        dialog.show();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void getVoucherData() {
        HashMap<String, String> params = new HashMap<>();
        params.put("voucher", user_voucher);
        params.put("amount", String.valueOf(0));
        Log.e("ERRdHashMap", params.toString());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.voucher_data), new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                closeLoadingPopup();
//                Log.d("CheckBalance Response", response.toString());
//                Log.e("ERRdCheckBalance", response.toString());
                try {
                    JSONObject data = null;
                    data = response.getJSONObject("data");
//                    Log.e("ERRdDATA1", data.toString());

                    JSONObject receiver = data.getJSONObject("receiver");
//                    Log.e("ERRdDATA2", data.toString());

                    JSONObject user = receiver.getJSONObject("user");
//                    Log.e("ERRdDATA3", data.toString());

                    String current_value = data.getString("current_value");
//                    Log.e("ERRdDATA4", data.toString());


                    user_id = receiver.getString("id");
                    user_email = user.getString("email");
                    user_index = receiver.getString("index");
                    user_phone = receiver.getString("phone");
//                    user_image = receiver.getString("image");

//                    toast(user_image);
//                    Bitmap upload_image = convertToBitmap(getDrawable(R.drawable.u    pload_img), 40, 40);

                        showVoucherData(user_index, user_email, user_phone);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("ERRdPrintStack", e.toString());

                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                closeLoadingPopup();
                String statusCode = String.valueOf(error.networkResponse.statusCode);
                String body;
                //get response body and parse with appropriate encoding
                if (error.networkResponse.data != null) {
                    try {
                        body = new String(error.networkResponse.data, "UTF-8");
                        JSONObject response = new JSONObject(body);
                        body = response.getString("message");
                        toast(body);
                        if (body.contains("Unauthenticated")) {
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


    private void showVoucherData(Bitmap image, String index, String email, String phone) {

        View v = getLayoutInflater().inflate(R.layout.voucher_data, null);

//        byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        EditText index_tv = v.findViewById(R.id.index);

        EditText email_tv = v.findViewById(R.id.emailEnter);
        if (email.equals("not provided")) {
            enable_email = true;
            email_tv.setEnabled(enable_email);

        } else {
            enable_email = false;
            email_tv.setText(email);
        }

        EditText phone_tv = v.findViewById(R.id.phoneEnter);
        if (phone.equals("not provided")) {
            enable_phone = true;
            phone_tv.setEnabled(enable_phone);

        } else {
            enable_phone = false;
            phone_tv.setText(phone);
        }


        CircularImageView circularImageView = v.findViewById(R.id.image);
        circularImageView.setImageBitmap(image);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v);

        AlertDialog dialog = builder.create();
        email_tv.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                user_email = s.toString();

                // you can call or do what you want with your EditText here

                // yourEditText...
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        phone_tv.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                user_phone = s.toString();

                // you can call or do what you want with your EditText here

                // yourEditText...
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        index_tv.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                user_index = s.toString();

                // you can call or do what you want with your EditText here

                // yourEditText...
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

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

            saleRequest(amt);
        });
        circularImageView.setOnClickListener(view -> {
            dispatchTakePictureIntent();
            dialog.dismiss();

        });

        closeLoadingPopup();

        dialog.show();
    }

    private void showVoucherData(String index, String email, String phone) {

        View v = getLayoutInflater().inflate(R.layout.voucher_data, null);

//        byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        EditText index_tv = v.findViewById(R.id.index);

        EditText email_tv = v.findViewById(R.id.emailEnter);
        if (email.equals("not provided")) {
            enable_email = true;
            email_tv.setEnabled(enable_email);


        } else {
            enable_email = false;
            email_tv.setText(email);
        }

        EditText phone_tv = v.findViewById(R.id.phoneEnter);
        if (phone.equals("not provided")) {
            enable_phone = true;
            phone_tv.setEnabled(enable_phone);

        } else {
            enable_phone = false;
            phone_tv.setText(phone);
        }

        CircularImageView circularImageView = v.findViewById(R.id.image);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(v);

        email_tv.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                user_email = s.toString();

                // you can call or do what you want with your EditText here

                // yourEditText...
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        phone_tv.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                user_phone = s.toString();

                // you can call or do what you want with your EditText here

                // yourEditText...
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        index_tv.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                user_index = s.toString();

                // you can call or do what you want with your EditText here

                // yourEditText...
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


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

            saleRequest(amt);
        });
        circularImageView.setOnClickListener(view -> {
            dispatchTakePictureIntent();
            dialog.dismiss();

        });

        closeLoadingPopup();

        dialog.show();
    }

    private void saleRequest(Double amount) {
        HashMap<String, String> params = new HashMap<>();
        params.put("voucher", user_voucher);
        params.put("amount", String.valueOf(amount));
        params.put("index", user_index);
        if (enable_email) {
            params.put("email", user_email);
        }
        if (enable_phone) {
            params.put("phone", user_phone);
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, getString(R.string.sale), new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("CheckBalance Response", response.toString());
                try {
                    printReceipt(user_voucher, response.getString("transaction_id"), amt, response.getDouble("remaining_balance"));
                    new Handler().postDelayed(closeLoadingPopup(), 2500);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                closeLoadingPopup();

                String statusCode = String.valueOf(error.networkResponse.statusCode);
                String body;
                //get response body and parse with appropriate encoding
                if (error.networkResponse.data != null) {
                    try {
                        body = new String(error.networkResponse.data, "UTF-8");
                        JSONObject response = new JSONObject(body);
                        body = response.getString("msg");
                        toast(body);
                        if (body.contains("Unauthenticated")) {
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


}
