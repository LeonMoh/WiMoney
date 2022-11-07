package com.example.wimoney;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class RegistrationActivity extends AppCompatActivity {
    String companyName, ownerName, nameInBank, accNumber, pw, cp, email;
    EditText cName, oName, aNumber, nBank, password, confirm, e;
    Button btRegistration, btBack;
    @SuppressLint({"MissingInflatedId", "CutPasteId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

         cName = findViewById(R.id.company_name);
         oName = findViewById(R.id.owner_name);
         aNumber = findViewById(R.id.account_number);
         nBank = findViewById(R.id.name_bank);
         password = findViewById(R.id.password);
         confirm = findViewById(R.id.confirmPassword);
         e = findViewById(R.id.email);
        btRegistration = findViewById(R.id.register);
        btBack = findViewById(R.id.back);

        btRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkRegistration();
            }
        });

        Button back = findViewById(R.id.back);
        back.setOnClickListener(view -> {
            Intent intent = new Intent(this, MerchantLogin.class);
            startActivity(intent);
            finish();
        });
    }

    private void checkRegistration(){
         companyName = cName.getText().toString();
         ownerName = oName.getText().toString();
         accNumber = aNumber.getText().toString();
         nameInBank = nBank.getText().toString();
         pw = password.getText().toString();
         cp = confirm.getText().toString();
         email = e.getText().toString();


        if (companyName.isEmpty() || pw.isEmpty() || email.isEmpty() || ownerName.isEmpty() || accNumber.isEmpty() || nameInBank.isEmpty()){
            alertFail("All fields are required.");
        }
        else if(!pw.equals(cp)){
            alertFail("Passwords differ.");
        } else {
            sendHome();
        }
    }

    private void alertSuccess(String s) {
        new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle("Success")
                .setIcon(R.drawable.ic_check)
                .setMessage(s)
                .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onBackPressed();
                    }
                }).show();
    }

    private void alertFail(String s) {
        new AlertDialog.Builder(RegistrationActivity.this)
                .setTitle("Failed")
                .setIcon(R.drawable.ic_warning)
                .setMessage(s)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void sendHome() {
        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}