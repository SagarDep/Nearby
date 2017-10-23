package cf.nearby.nearby.nurse;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import cf.nearby.nearby.BaseActivity;
import cf.nearby.nearby.Information;
import cf.nearby.nearby.R;
import cf.nearby.nearby.StartActivity;
import cf.nearby.nearby.activity.CameraActivity;
import cf.nearby.nearby.activity.RecordMealActivity;
import cf.nearby.nearby.activity.RecordMedicineActivity;
import cf.nearby.nearby.activity.RecordRemarkActivity;
import cf.nearby.nearby.activity.RecordVitalSignActivity;
import cf.nearby.nearby.obj.HaveMeal;
import cf.nearby.nearby.obj.Patient;
import cf.nearby.nearby.obj.PatientRemark;
import cf.nearby.nearby.obj.TakeMedicine;
import cf.nearby.nearby.obj.VitalSign;
import cf.nearby.nearby.util.AdditionalFunc;
import cf.nearby.nearby.util.ParsePHP;

public class NurseRecordActivity extends BaseActivity {

    private MyHandler handler = new MyHandler();
    private final int MSG_MESSAGE_SUCCESS = 500;
    private final int MSG_MESSAGE_FAIL = 501;

    public static final int UPDATE_VITAL = 401;
    public static final int UPDATE_MEDICINE = 402;
    public static final int UPDATE_MEAL = 403;
    public static final int UPDATE_REMARK = 404;
    public static final int UPDATE_PHOTO = 405;

    private Button saveBtn;

    private MaterialDialog progressDialog;

    private Patient selectedPatient;
    private VitalSign vitalSign;
    private ArrayList<TakeMedicine> takeMedicines;
    private ArrayList<PatientRemark> remarks;
    private HaveMeal haveMeal;
//    private byte[] photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_record);

        selectedPatient = (Patient)getIntent().getSerializableExtra("patient");
        vitalSign = new VitalSign();
        haveMeal = new HaveMeal();
        takeMedicines = new ArrayList<>();
        remarks = new ArrayList<>();
        CameraActivity.photo = new byte[0];
//        photo = new byte[0];

        init();

    }

    private void save(){

        HashMap<String, String> map = new HashMap<>();
        map.put("service", "recordData");
        map.put("patient_id", selectedPatient.getId());
        map.put("employee_id", StartActivity.employee.getId());
        map.put("location_id", StartActivity.employee.getLocation().getId());

        if(!haveMeal.isEmpty()){
            map.put("have_meal", "1");
            map.put("have_meal_type", haveMeal.getType());
            map.put("have_meal_description", haveMeal.getDescription());
        }

        if(!vitalSign.isEmpty()){
            map.put("vital_sign", "1");
            map.put("vital_sign_bp_max", (vitalSign.getBpMax() == null) ? "0" : Double.toString(vitalSign.getBpMax()));
            map.put("vital_sign_bp_min", (vitalSign.getBpMin() == null) ? "0" : Double.toString(vitalSign.getBpMin()));
            map.put("vital_sign_pulse", (vitalSign.getPulse() == null) ? "0" : Double.toString(vitalSign.getPulse()));
            map.put("vital_sign_temperature", (vitalSign.getTemperature() == null) ? "0" : Double.toString(vitalSign.getTemperature()));
        }

        if(remarks.size() > 0){
            map.put("remarks", "1");
            map.put("remarks_size", Integer.toString(remarks.size()));
            for(int i=0; i<remarks.size(); i++){
                map.put("remarks_description" + i, AdditionalFunc.replaceNewLineString(remarks.get(i).getDescription().toString()));
            }
        }

        if(takeMedicines.size() > 0){
            map.put("take_medicines", "1");
            map.put("take_medicines_size", Integer.toString(takeMedicines.size()));
            for(int i=0; i<takeMedicines.size(); i++){
                map.put("take_medicines_patient_medicine_id" + i, takeMedicines.get(i).getPatientMedicine().getId());
            }
        }

        progressDialog.show();

        new ParsePHP(Information.MAIN_SERVER_ADDRESS, map){
            @Override
            protected void afterThreadFinish(String data) {

                try {
                    JSONObject jObj = new JSONObject(data);
                    String status = jObj.getString("status");

                    if("success".equals(status)){
                        handler.sendMessage(handler.obtainMessage(MSG_MESSAGE_SUCCESS));
                    }else{
                        handler.sendMessage(handler.obtainMessage(MSG_MESSAGE_FAIL));
                    }

                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    handler.sendMessage(handler.obtainMessage(MSG_MESSAGE_FAIL));
                }

            }
        }.start();


    }

    private void init(){

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        progressDialog = new MaterialDialog.Builder(this)
                .content(R.string.please_wait)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .theme(Theme.LIGHT)
                .cancelable(false)
                .build();

        saveBtn = (Button)findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });

        // Menu Buttons
        findViewById(R.id.cv_record_vital_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NurseRecordActivity.this, RecordVitalSignActivity.class);
                intent.putExtra("vital_sign", vitalSign);
                startActivityForResult(intent, UPDATE_VITAL);
            }
        });
        findViewById(R.id.cv_record_patient_medicine).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NurseRecordActivity.this, RecordMedicineActivity.class);
                intent.putExtra("take_medicines", takeMedicines);
                intent.putExtra("patient", selectedPatient);
                startActivityForResult(intent, UPDATE_MEDICINE);
            }
        });
        findViewById(R.id.cv_record_patient_meal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NurseRecordActivity.this, RecordMealActivity.class);
                intent.putExtra("have_meal", haveMeal);
                startActivityForResult(intent, UPDATE_MEAL);
            }
        });
        findViewById(R.id.cv_record_remark).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NurseRecordActivity.this, RecordRemarkActivity.class);
                intent.putExtra("remarks", remarks);
                intent.putExtra("patient", selectedPatient);
                startActivityForResult(intent, UPDATE_REMARK);
            }
        });
        findViewById(R.id.cv_record_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NurseRecordActivity.this, CameraActivity.class);
//                intent.putExtra("photo", photo);
                startActivityForResult(intent, UPDATE_PHOTO);
            }
        });

        // Profile
        ImageView img = (ImageView)findViewById(R.id.img);
        if(selectedPatient.getPic() != null && !"".equals(selectedPatient.getPic())) {
            Picasso.with(getApplicationContext())
                    .load(selectedPatient.getPic())
                    .into(img);
        }
        TextView tv_name = (TextView)findViewById(R.id.tv_name);
        TextView tv_dob = (TextView)findViewById(R.id.tv_dob);
        TextView tv_registeredDate = (TextView)findViewById(R.id.tv_registered_date);
        tv_name.setText(selectedPatient.getName());
        tv_dob.setText(AdditionalFunc.getDateString(selectedPatient.getDob()));
        tv_registeredDate.setText(AdditionalFunc.getDateString(selectedPatient.getRegisteredDate()));


    }

    private void changeBtnColor(CardView cv, boolean check){

        if(check){
            cv.setCardBackgroundColor(getColorId(R.color.pastel_green));
            for(int i=0; i<cv.getChildCount(); i++){
                ViewGroup vg = (ViewGroup)cv.getChildAt(i);
                for(int j=0; j<vg.getChildCount(); j++){
                    View v = vg.getChildAt(j);
                    if(v instanceof TextView){
                        ((TextView) v).setTextColor(getColorId(R.color.white));
                    }
                }
            }
        }else{
            cv.setCardBackgroundColor(getColorId(R.color.white));
            for(int i=0; i<cv.getChildCount(); i++){
                ViewGroup vg = (ViewGroup)cv.getChildAt(i);
                for(int j=0; j<vg.getChildCount(); j++){
                    View v = vg.getChildAt(j);
                    if(v instanceof TextView){
                        ((TextView) v).setTextColor(getColorId(R.color.dark_gray));
                    }
                }
            }
        }

    }

    private class MyHandler extends Handler implements Serializable {

        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_MESSAGE_SUCCESS:
                    progressDialog.hide();
                    new MaterialDialog.Builder(NurseRecordActivity.this)
                            .title(R.string.success_srt)
                            .content(R.string.successfully_recorded)
                            .positiveText(R.string.ok)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                    NurseRecordActivity.this.finish();
                                }
                            })
                            .show();
                    break;
                case MSG_MESSAGE_FAIL:
                    progressDialog.hide();
                    new MaterialDialog.Builder(NurseRecordActivity.this)
                            .title(R.string.fail_srt)
                            .positiveText(R.string.ok)
                            .show();
                    break;
                default:
                    break;
            }
        }
    }

    private void checkChangeBtn(){

        // vital sign
        boolean isVital = !vitalSign.isEmpty();
        changeBtnColor((CardView)findViewById(R.id.cv_record_vital_sign), isVital);
        // 복용약 여부
        boolean isTakeMedicine = takeMedicines != null && takeMedicines.size() > 0;
        changeBtnColor((CardView)findViewById(R.id.cv_record_patient_medicine), isTakeMedicine);
        // 식사 여부
        boolean isHaveMeal = haveMeal.getType() != null && !haveMeal.getType().isEmpty();
        changeBtnColor((CardView)findViewById(R.id.cv_record_patient_meal), isHaveMeal);
        // 특이사항
        boolean isRemark = remarks != null && remarks.size() > 0;
        changeBtnColor((CardView)findViewById(R.id.cv_record_remark), isRemark);
        // 사진
        boolean isPhoto = CameraActivity.photo.length > 0;
        changeBtnColor((CardView)findViewById(R.id.cv_record_photo), isPhoto);

        boolean status = isVital || isTakeMedicine || isHaveMeal || isRemark || isPhoto;

        saveBtn.setEnabled(status);
        if(status){
            saveBtn.setBackgroundColor(getColorId(R.color.colorPrimary));
        }else{
            saveBtn.setBackgroundColor(getColorId(R.color.dark_gray));
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case UPDATE_VITAL:
                if(data != null){
                    vitalSign = (VitalSign)data.getSerializableExtra("vital_sign");
                    checkChangeBtn();
                }
                break;
            case UPDATE_MEAL:
                if(data != null){
                    haveMeal = (HaveMeal)data.getSerializableExtra("have_meal");
                    checkChangeBtn();
                }
                break;
            case UPDATE_MEDICINE:
                if(data != null){
                    takeMedicines = (ArrayList<TakeMedicine>)data.getSerializableExtra("take_medicines");
                    checkChangeBtn();
                }
                break;
            case UPDATE_REMARK:
                if(data != null){
                    remarks = (ArrayList<PatientRemark>)data.getSerializableExtra("remarks");
                    checkChangeBtn();
                }
                break;
            case UPDATE_PHOTO:
//                if(data != null){
////                    photo = data.getByteArrayExtra("photo");
//
//                }
                checkChangeBtn();
                break;
            default:
                break;
        }

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

}
