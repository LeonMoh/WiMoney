package com.example.wimoney;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;
import com.topwise.cloudpos.aidl.AidlDeviceService;
import com.topwise.cloudpos.aidl.printer.AidlPrinter;
import com.topwise.cloudpos.aidl.printer.AidlPrinterListener;
import com.topwise.cloudpos.aidl.printer.PrintItemObj;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private PopupWindow popupWindow;
    public static final String TOPWISE_SERVICE_ACTION = "topwise_cloudpos_device_service";
    private static final String TAG = "BaseActivity";
    private AidlPrinter printerDev = null;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            if (serviceBinder != null) {
                onDeviceConnected(AidlDeviceService.Stub.asInterface(serviceBinder));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);


        Hawk.init(this).build();

        showFullscreen();
    }

    @Override
    protected void onResume() {
        showFullscreen();
        bindService();
        super.onResume();
    }

    @Override
    protected void onStop() {
        if (popupWindow != null && popupWindow.isShowing())
            popupWindow.dismiss();

        unbindService(connection);
        super.onStop();
    }

    public void bindService() {
        Intent intent = new Intent();
        intent.setAction(TOPWISE_SERVICE_ACTION);
        final Intent eintent = new Intent(createExplicitFromImplicitIntent(this, intent));
        boolean flag = bindService(eintent, connection, Context.BIND_AUTO_CREATE);
        if (flag) {
            Log.d(TAG, "服务绑定成功");
        } else {
            Log.d(TAG, "服务绑定失败");
        }
    }

    public void showFullscreen() {
        View v = getWindow().getDecorView();
        v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public void showLoadingPopup(View parent) {
        View v = getLayoutInflater().inflate(R.layout.loading_popup, null);
        popupWindow = new PopupWindow(v, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, false);
        popupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0);
    }

    public void closeLoadingPopup() {
        if (popupWindow != null && popupWindow.isShowing())
            popupWindow.dismiss();
    }

    public void printReceipt(String voucher, String trxnId, double total, double balance) {
        String datetime = getDate() + " " + getTime();
        ArrayList<PrintItemObj> itemObjs = new ArrayList<>();
        itemObjs.add(new PrintItemObj("Receipt", 24, true, PrintItemObj.ALIGN.CENTER));
        itemObjs.add(new PrintItemObj("", 12, true, PrintItemObj.ALIGN.CENTER));
        itemObjs.add(new PrintItemObj("Trxn ID: " + trxnId, 18, false, PrintItemObj.ALIGN.LEFT));
        itemObjs.add(new PrintItemObj("Date: " + datetime, 18, false, PrintItemObj.ALIGN.LEFT));
        itemObjs.add(new PrintItemObj("Total: $" + String.format(Locale.ENGLISH, "%.2f", total), 18, false, PrintItemObj.ALIGN.LEFT));
        itemObjs.add(new PrintItemObj("", 12, true, PrintItemObj.ALIGN.CENTER));
        itemObjs.add(new PrintItemObj("Voucher used: " + voucher.replace(voucher.substring(0, 8), "********"), 12, true, PrintItemObj.ALIGN.LEFT));
        itemObjs.add(new PrintItemObj("Balance: " + String.format(Locale.ENGLISH, "%.2f", balance), 18, true, PrintItemObj.ALIGN.LEFT));
        itemObjs.add(new PrintItemObj("", 12, true, PrintItemObj.ALIGN.CENTER));
        itemObjs.add(new PrintItemObj("Thank you for your purchase", 18, true, PrintItemObj.ALIGN.CENTER));
        itemObjs.add(new PrintItemObj("\n\n", 12, true, PrintItemObj.ALIGN.CENTER));

        try {
            printerDev.printText(itemObjs, new AidlPrinterListener.Stub() {
                @Override
                public void onError(int i) throws RemoteException {
                    runOnUiThread(() -> toast("Printing error occurred"));
                }

                @Override
                public void onPrintFinish() throws RemoteException {
                    runOnUiThread(() -> showPrintMerchantCopyAlert(voucher, trxnId, total, datetime));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void printMerchantReceipt(String voucher, String trxnId, double total, String datetime) {
        ArrayList<PrintItemObj> itemObjs = new ArrayList<>();
        itemObjs.add(new PrintItemObj("Merchant Copy", 24, true, PrintItemObj.ALIGN.CENTER));
        itemObjs.add(new PrintItemObj("", 12, true, PrintItemObj.ALIGN.CENTER));
        itemObjs.add(new PrintItemObj("Trxn ID: " + trxnId, 18, false, PrintItemObj.ALIGN.LEFT));
        itemObjs.add(new PrintItemObj("Date: " + datetime, 18, false, PrintItemObj.ALIGN.LEFT));
        itemObjs.add(new PrintItemObj("Total: $" + String.format(Locale.ENGLISH, "%.2f", total), 18, false, PrintItemObj.ALIGN.LEFT));
        itemObjs.add(new PrintItemObj("", 12, true, PrintItemObj.ALIGN.CENTER));
        itemObjs.add(new PrintItemObj("Voucher used: " + voucher.replace(voucher.substring(0, 8), "********"), 12, true, PrintItemObj.ALIGN.LEFT));
        itemObjs.add(new PrintItemObj("\n\n", 12, true, PrintItemObj.ALIGN.CENTER));

        try {
            printerDev.printText(itemObjs, new AidlPrinterListener.Stub() {
                @Override
                public void onError(int i) throws RemoteException {
                    runOnUiThread(() -> toast("Printing error occurred"));
                }

                @Override
                public void onPrintFinish() throws RemoteException {
                    runOnUiThread(() -> showCompletedAlert());
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private String getDate() {
        Date c = Calendar.getInstance().getTime();

        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        return df.format(c);
    }

    private String getTime() {
        Date c = Calendar.getInstance().getTime();

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss a");
        return df.format(c);
    }

    public int getMerchantId() {
        return Hawk.get("access_token", 0);
    }

    private void showPrintMerchantCopyAlert(String voucher, String trxnId, double total, String datetime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Transaction Completed");

        builder.setPositiveButton("Print Merchant Copy", (dialog, id) -> {
            printMerchantReceipt(voucher, trxnId, total, datetime);
        });

        AlertDialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        dialog.show();
    }

    private void showCompletedAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Transaction Completed");

        builder.setPositiveButton("Confirm", (dialog, id) -> {
            Hawk.delete("amount");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        AlertDialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        dialog.show();
    }

    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    private void onDeviceConnected(AidlDeviceService serviceManager) {
        try {
            printerDev = AidlPrinter.Stub.asInterface(serviceManager.getPrinter());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isTokenRequired() {
        int s = Hawk.get("access_token", 0);
        if (s == 0)
            return true;
        return false;
    }
}
