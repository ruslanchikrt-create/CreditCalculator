package com.example.creditcalculator;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class AddReminderActivity extends AppCompatActivity {
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_PRINCIPAL = "principal";
    public static final String EXTRA_BASE_AMOUNT = "base_amount";
    public static final String EXTRA_DOWN_PAYMENT = "down_payment";
    public static final String EXTRA_INSURANCE = "insurance";
    public static final String EXTRA_INSURANCE_FINANCED = "insurance_financed";
    public static final String EXTRA_RATE = "rate";
    public static final String EXTRA_MONTHS = "months";
    public static final String EXTRA_PAYMENT = "payment";
    public static final String EXTRA_PAYMENT_TYPE = "payment_type";
    public static final String EXTRA_EDIT_ID = "edit_reminder_id";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 3010;

    private ScrollView formScroll;
    private Spinner typeSpinner, termUnitSpinner, daysSpinner, paymentTypeSpinner;
    private ArrayAdapter<String> termUnitAdapter;
    private TextInputLayout principalLayout, downPaymentLayout, insuranceLayout, rateLayout, paymentLayout, dateLayout, timeLayout;
    private TextInputEditText titleInput, principalInput, downPaymentInput, insuranceInput, rateInput, termInput, paymentInput, dateInput, timeInput;
    private SwitchMaterial insuranceFinancedSwitch;
    private Calendar selectedDate;
    private int selectedHour = 9, selectedMinute = 0;
    private boolean updatingPayment, updatingTitle, titleEditedByUser;
    private ReminderScheduler.PaymentReminder editReminder;

    @Override protected void attachBaseContext(Context newBase) { super.attachBaseContext(AppPreferences.wrapLocale(newBase)); }

    @Override public void onCreate(Bundle b) {
        AppPreferences.applyNightMode(this); super.onCreate(b); WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        long editId = getIntent().getLongExtra(EXTRA_EDIT_ID, -1L); if (editId > 0) editReminder = ReminderScheduler.findById(this, editId);
        setContentView(buildContent()); setupSpinners(); setupListeners(); applyInitialValues();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); UiUtils.applyBackground(this, root);
        LinearLayout bar = new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(4),0,dp(12),0); bar.setBackgroundColor(ContextCompat.getColor(this,R.color.primary)); root.addView(bar,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView back = top("‹",34); back.setOnClickListener(v->finish()); bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView bt = top(editReminder==null?AppPreferences.tr(this,"Новое напоминание","New reminder"):AppPreferences.tr(this,"Редактировать запись","Edit item"),20); bt.setTypeface(null,android.graphics.Typeface.BOLD); bt.setGravity(Gravity.CENTER_VERTICAL); bar.addView(bt,new LinearLayout.LayoutParams(0,-1,1f));

        formScroll = new ScrollView(this); formScroll.setFillViewport(true); formScroll.setClipToPadding(false); root.addView(formScroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20),dp(22),dp(20),dp(32)); formScroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        content.addView(text(AppPreferences.tr(this,"Добавить платёж","Add payment"),28,R.color.text_main,true));
        TextView sub=text(AppPreferences.tr(this,"Приложение рассчитает график, отследит оплату и напомнит заранее и в день платежа.","The app will calculate the schedule, track payments and remind you before and on the due date."),14,R.color.text_secondary,false); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,dp(6),0,dp(18));content.addView(sub,sp);

        addLabel(content,AppPreferences.tr(this,"Тип","Type")); typeSpinner=createSpinner(); content.addView(typeSpinner,spinnerParams());
        titleInput=addField(content,AppPreferences.tr(this,"Название","Name"),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        principalLayout=addLayout(content,AppPreferences.tr(this,"Сумма кредита, ₽","Loan amount, ₽")); principalInput=addInput(principalLayout,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        downPaymentLayout=addLayout(content,AppPreferences.tr(this,"Первоначальный взнос, ₽","Down payment, ₽")); downPaymentInput=addInput(downPaymentLayout,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        insuranceLayout=addLayout(content,AppPreferences.tr(this,"Страховка, ₽","Insurance, ₽")); insuranceInput=addInput(insuranceLayout,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        insuranceFinancedSwitch=new SwitchMaterial(this); insuranceFinancedSwitch.setText(AppPreferences.tr(this,"Включить страховку в сумму кредита","Include insurance in financed amount")); insuranceFinancedSwitch.setTextColor(ContextCompat.getColor(this,R.color.text_main)); insuranceFinancedSwitch.setTextSize(14); insuranceFinancedSwitch.setChecked(true); LinearLayout.LayoutParams isp=new LinearLayout.LayoutParams(-1,-2);isp.setMargins(0,dp(-4),0,dp(10));content.addView(insuranceFinancedSwitch,isp);
        rateLayout=addLayout(content,AppPreferences.tr(this,"Процентная ставка, % годовых","Interest rate, % per year")); rateInput=addInput(rateLayout,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addTermRow(content);
        addLabel(content,AppPreferences.tr(this,"Тип платежей","Payment type")); paymentTypeSpinner=createSpinner(); content.addView(paymentTypeSpinner,spinnerParams());
        paymentLayout=addLayout(content,AppPreferences.tr(this,"Ежемесячный платёж, ₽ (можно изменить)","Monthly payment, ₽ (editable)")); paymentInput=addInput(paymentLayout,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        dateLayout=addLayout(content,AppPreferences.tr(this,"Дата первого платежа","First payment date")); dateInput=addInput(dateLayout,InputType.TYPE_NULL); dateInput.setFocusable(false);dateInput.setClickable(true);dateInput.setOnClickListener(v->pickDate());
        timeLayout=addLayout(content,AppPreferences.tr(this,"Время напоминания","Reminder time")); timeInput=addInput(timeLayout,InputType.TYPE_NULL); timeInput.setFocusable(false);timeInput.setClickable(true);timeInput.setText("09:00");timeInput.setOnClickListener(v->pickTime());
        addLabel(content,AppPreferences.tr(this,"Напомнить до платежа","Remind before payment")); daysSpinner=createSpinner(); content.addView(daysSpinner,spinnerParams());

        MaterialButton save=new MaterialButton(this);save.setText(AppPreferences.tr(this,"Сохранить","Save"));save.setAllCaps(false);save.setTextSize(17);save.setTextColor(Color.WHITE);save.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));save.setCornerRadius(dp(14));save.setOnClickListener(v->save());LinearLayout.LayoutParams sv=new LinearLayout.LayoutParams(-1,dp(56));sv.setMargins(0,dp(8),0,dp(16));content.addView(save,sv);

        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());v.setPadding(0,bars.top,0,0);formScroll.setPadding(0,0,0,Math.max(bars.bottom,ime.bottom)+dp(24));return insets;}); return root;
    }

    private void addTermRow(LinearLayout p){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,0,0,dp(12));p.addView(row,rp);TextInputLayout l=createLayout(AppPreferences.tr(this,"Срок","Term"));row.addView(l,new LinearLayout.LayoutParams(0,-2,1f));termInput=createInput(InputType.TYPE_CLASS_NUMBER);l.addView(termInput,new TextInputLayout.LayoutParams(-1,-2));termUnitSpinner=createSpinner();LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(dp(136),dp(58));up.setMargins(dp(8),0,0,0);row.addView(termUnitSpinner,up);}

    private void setupSpinners(){String[] types={FormatUtils.typeLabel(this,ReminderScheduler.TYPE_CREDIT),FormatUtils.typeLabel(this,ReminderScheduler.TYPE_MORTGAGE),FormatUtils.typeLabel(this,ReminderScheduler.TYPE_AUTO),FormatUtils.typeLabel(this,ReminderScheduler.TYPE_INSTALLMENT),FormatUtils.typeLabel(this,ReminderScheduler.TYPE_DEPOSIT)};typeSpinner.setAdapter(UiUtils.spinnerAdapter(this,types));termUnitAdapter=UiUtils.spinnerAdapter(this,new ArrayList<>(Arrays.asList(UiUtils.termUnit(this,1,false),UiUtils.termUnit(this,1,true))));termUnitSpinner.setAdapter(termUnitAdapter);paymentTypeSpinner.setAdapter(UiUtils.spinnerAdapter(this,new String[]{AppPreferences.tr(this,"Аннуитетный","Annuity"),AppPreferences.tr(this,"Дифференцированный","Differential")}));String[] days=new String[7];for(int i=0;i<7;i++){int d=i+1;days[i]=AppPreferences.isEnglish(this)?d+(d==1?" day before":" days before"):AppPreferences.isTurkish(this)?d+" gün önce":AppPreferences.isSpanish(this)?d+" días antes":"За "+d+" "+russianDays(d);}daysSpinner.setAdapter(UiUtils.spinnerAdapter(this,days));daysSpinner.setSelection(2);}

    private void setupListeners(){MoneyWatcher money=new MoneyWatcher(this::updatePayment);principalInput.addTextChangedListener(money);downPaymentInput.addTextChangedListener(new MoneyWatcher(this::updatePayment));insuranceInput.addTextChangedListener(new MoneyWatcher(this::updatePayment));rateInput.addTextChangedListener(new SimpleWatcher(this::updatePayment));termInput.addTextChangedListener(new SimpleWatcher(()->{updateUnits();updatePayment();}));insuranceFinancedSwitch.setOnCheckedChangeListener((b,c)->updatePayment());paymentTypeSpinner.setOnItemSelectedListener(new SimpleSelected(this::updatePayment));termUnitSpinner.setOnItemSelectedListener(new SimpleSelected(this::updatePayment));typeSpinner.setOnItemSelectedListener(new SimpleSelected(()->{updateFields(true);updatePayment();}));titleInput.addTextChangedListener(new SimpleWatcher(()->{if(!updatingTitle)titleEditedByUser=true;}));}

    private void updateFields(boolean updateTitle){String type=FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition());boolean credit=ReminderScheduler.TYPE_CREDIT.equals(type), mortgage=ReminderScheduler.TYPE_MORTGAGE.equals(type),auto=ReminderScheduler.TYPE_AUTO.equals(type),inst=ReminderScheduler.TYPE_INSTALLMENT.equals(type),deposit=ReminderScheduler.TYPE_DEPOSIT.equals(type);downPaymentLayout.setVisibility((mortgage||auto||inst)?View.VISIBLE:View.GONE);insuranceLayout.setVisibility(deposit?View.GONE:View.VISIBLE);insuranceFinancedSwitch.setVisibility(deposit?View.GONE:View.VISIBLE);rateLayout.setVisibility(inst?View.GONE:View.VISIBLE);paymentLayout.setVisibility(deposit?View.GONE:View.VISIBLE);paymentTypeSpinner.setVisibility((credit||mortgage||auto)?View.VISIBLE:View.GONE);if(inst)insuranceFinancedSwitch.setText(AppPreferences.tr(this,"Включить страховку в рассрочку","Include insurance in installment"));else insuranceFinancedSwitch.setText(AppPreferences.tr(this,"Включить страховку в сумму кредита","Include insurance in financed amount"));if(credit)principalLayout.setHint(AppPreferences.tr(this,"Сумма кредита, ₽","Loan amount, ₽"));else if(mortgage)principalLayout.setHint(AppPreferences.tr(this,"Стоимость жилья, ₽","Property price, ₽"));else if(auto)principalLayout.setHint(AppPreferences.tr(this,"Стоимость автомобиля, ₽","Car price, ₽"));else if(inst)principalLayout.setHint(AppPreferences.tr(this,"Стоимость покупки, ₽","Purchase price, ₽"));else principalLayout.setHint(AppPreferences.tr(this,"Сумма вклада, ₽","Deposit amount, ₽"));dateLayout.setHint(deposit?AppPreferences.tr(this,"Дата открытия вклада","Deposit start date"):AppPreferences.tr(this,"Дата первого платежа","First payment date"));if(updateTitle&&!titleEditedByUser)setDefaultTitle(type);}

    private void applyInitialValues(){if(editReminder!=null){typeSpinner.setSelection(FormatUtils.typePosition(editReminder.type));updateFields(false);updatingTitle=true;titleInput.setText(editReminder.title);updatingTitle=false;titleEditedByUser=true;principalInput.setText(format(editReminder.baseAmount));if(editReminder.downPayment>0)downPaymentInput.setText(format(editReminder.downPayment));if(editReminder.insurance>0)insuranceInput.setText(format(editReminder.insurance));insuranceFinancedSwitch.setChecked(editReminder.insuranceFinanced);rateInput.setText(trim(editReminder.annualRate));if(editReminder.months%12==0){termInput.setText(String.valueOf(editReminder.months/12));termUnitSpinner.setSelection(1);}else{termInput.setText(String.valueOf(editReminder.months));termUnitSpinner.setSelection(0);}paymentTypeSpinner.setSelection(ReminderScheduler.PAYMENT_DIFFERENTIAL.equals(editReminder.paymentType)?1:0);selectedDate=Calendar.getInstance();selectedDate.setTimeInMillis(editReminder.firstPaymentMillis);dateInput.setText(FormatUtils.date(this,editReminder.firstPaymentMillis));selectedHour=editReminder.reminderHour;selectedMinute=editReminder.reminderMinute;timeInput.setText(String.format(java.util.Locale.US,"%02d:%02d",selectedHour,selectedMinute));daysSpinner.setSelection(Math.max(0,Math.min(6,editReminder.daysBefore-1)));if(editReminder.amount>0)paymentInput.setText(format(editReminder.amount));updateFields(false);return;}
        String t=getIntent().getStringExtra(EXTRA_TYPE);if(t!=null)typeSpinner.setSelection(FormatUtils.typePosition(t));updateFields(false);titleEditedByUser=false;setDefaultTitle(FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition()));double legacy=getIntent().getDoubleExtra(EXTRA_PRINCIPAL,0),base=getIntent().getDoubleExtra(EXTRA_BASE_AMOUNT,legacy),down=getIntent().getDoubleExtra(EXTRA_DOWN_PAYMENT,0),ins=getIntent().getDoubleExtra(EXTRA_INSURANCE,0),rate=getIntent().getDoubleExtra(EXTRA_RATE,0),pay=getIntent().getDoubleExtra(EXTRA_PAYMENT,0);int months=getIntent().getIntExtra(EXTRA_MONTHS,0);if(base>0)principalInput.setText(format(base));if(down>0)downPaymentInput.setText(format(down));if(ins>0)insuranceInput.setText(format(ins));insuranceFinancedSwitch.setChecked(getIntent().getBooleanExtra(EXTRA_INSURANCE_FINANCED,true));if(rate>0)rateInput.setText(trim(rate));if(months>0&&months%12==0){termInput.setText(String.valueOf(months/12));termUnitSpinner.setSelection(1);}else if(months>0)termInput.setText(String.valueOf(months));String pt=getIntent().getStringExtra(EXTRA_PAYMENT_TYPE);paymentTypeSpinner.setSelection(ReminderScheduler.PAYMENT_DIFFERENTIAL.equals(pt)?1:0);if(pay>0)paymentInput.setText(format(pay));else updatePayment();}

    private double financedPrincipal(){String type=FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition());double base=positive(principalInput);double down=downPaymentLayout.getVisibility()==View.VISIBLE?nonNeg(downPaymentInput):0;double ins=insuranceLayout.getVisibility()==View.VISIBLE?nonNeg(insuranceInput):0;if(!ReminderScheduler.TYPE_DEPOSIT.equals(type)&&down>=base)throw new IllegalArgumentException();double p=base-down+(insuranceFinancedSwitch.getVisibility()==View.VISIBLE&&insuranceFinancedSwitch.isChecked()?ins:0);if(p<=0)throw new IllegalArgumentException();return p;}
    private int months(){int v=Integer.parseInt(text(termInput).trim());if(v<=0)throw new IllegalArgumentException();return termUnitSpinner.getSelectedItemPosition()==1?v*12:v;}
    private void updatePayment(){if(updatingPayment||paymentInput==null)return;try{String type=FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition());if(ReminderScheduler.TYPE_DEPOSIT.equals(type))return;double p=financedPrincipal();int m=months();double rate=ReminderScheduler.TYPE_INSTALLMENT.equals(type)?0:nonNeg(rateInput);double value;if(ReminderScheduler.TYPE_INSTALLMENT.equals(type))value=p/m;else if(paymentTypeSpinner.getSelectedItemPosition()==1)value=ReminderScheduler.differentialFirstPayment(p,m,rate);else value=ReminderScheduler.annuity(p,m,rate);updatingPayment=true;paymentInput.setText(format(value));updatingPayment=false;}catch(Exception ignored){}}
    private void updateUnits(){if(termUnitAdapter==null)return;int val=1;try{val=Math.max(1,Integer.parseInt(text(termInput).trim()));}catch(Exception ignored){}int sel=termUnitSpinner.getSelectedItemPosition();termUnitAdapter.clear();termUnitAdapter.add(UiUtils.termUnit(this,val,false));termUnitAdapter.add(UiUtils.termUnit(this,val,true));termUnitAdapter.notifyDataSetChanged();termUnitSpinner.setSelection(Math.max(0,sel));}
    private void setDefaultTitle(String type){updatingTitle=true;titleInput.setText(FormatUtils.typeLabel(this,type));updatingTitle=false;}

    private void pickDate(){Calendar b=selectedDate==null?Calendar.getInstance():selectedDate;new DatePickerDialog(this,(d,y,m,day)->{Calendar v=Calendar.getInstance();v.clear();v.set(y,m,day,selectedHour,selectedMinute,0);selectedDate=v;dateInput.setText(FormatUtils.date(this,v.getTimeInMillis()));},b.get(Calendar.YEAR),b.get(Calendar.MONTH),b.get(Calendar.DAY_OF_MONTH)).show();}
    private void pickTime(){new TimePickerDialog(this,(v,h,m)->{selectedHour=h;selectedMinute=m;timeInput.setText(String.format(java.util.Locale.US,"%02d:%02d",h,m));if(selectedDate!=null){selectedDate.set(Calendar.HOUR_OF_DAY,h);selectedDate.set(Calendar.MINUTE,m);}},selectedHour,selectedMinute,true).show();}

    private void save(){try{String type=FormatUtils.typeCodeByPosition(typeSpinner.getSelectedItemPosition());String title=text(titleInput).trim();if(title.isEmpty())title=FormatUtils.typeLabel(this,type);double base=positive(principalInput),down=downPaymentLayout.getVisibility()==View.VISIBLE?nonNeg(downPaymentInput):0,ins=insuranceLayout.getVisibility()==View.VISIBLE?nonNeg(insuranceInput):0,principal=financedPrincipal();double rate=ReminderScheduler.TYPE_INSTALLMENT.equals(type)?0:nonNeg(rateInput);int m=months(),days=daysSpinner.getSelectedItemPosition()+1;String paymentType=(paymentTypeSpinner.getVisibility()==View.VISIBLE&&paymentTypeSpinner.getSelectedItemPosition()==1)?ReminderScheduler.PAYMENT_DIFFERENTIAL:ReminderScheduler.PAYMENT_ANNUITY;double payment=ReminderScheduler.TYPE_DEPOSIT.equals(type)?0:positive(paymentInput);if(selectedDate==null){Toast.makeText(this,AppPreferences.tr(this,ReminderScheduler.TYPE_DEPOSIT.equals(type)?"Выберите дату открытия вклада":"Выберите дату первого платежа",ReminderScheduler.TYPE_DEPOSIT.equals(type)?"Choose the deposit start date":"Choose the first payment date"),Toast.LENGTH_SHORT).show();return;}selectedDate.set(Calendar.HOUR_OF_DAY,selectedHour);selectedDate.set(Calendar.MINUTE,selectedMinute);long id=editReminder==null?System.currentTimeMillis():editReminder.id;ReminderScheduler.PaymentReminder r=new ReminderScheduler.PaymentReminder(id,type,title,base,down,ins,insuranceFinancedSwitch.isChecked(),principal,rate,payment,paymentType,selectedDate.getTimeInMillis(),m,days,selectedHour,selectedMinute);if(editReminder==null){ReminderScheduler.add(this,r);requestNotificationPermissionIfNeeded();if(hasPastPayments(r)){showPastDialog(r.id);return;}}else ReminderScheduler.updateEdited(this,r);finishSaved();}catch(Exception e){Toast.makeText(this,AppPreferences.tr(this,"Проверьте заполненные поля","Check the entered values"),Toast.LENGTH_SHORT).show();}}
    private boolean hasPastPayments(ReminderScheduler.PaymentReminder r){for(int i=0;i<r.months;i++){Calendar d=ReminderScheduler.buildDueDate(r,i);d.set(Calendar.HOUR_OF_DAY,23);d.set(Calendar.MINUTE,59);if(d.getTimeInMillis()<System.currentTimeMillis())return true;}return false;}
    private void showPastDialog(long id){new AlertDialog.Builder(this).setTitle(AppPreferences.tr(this,"Прошедшие платежи","Past payments")).setMessage(AppPreferences.tr(this,"Прошедшие платежи были оплачены по графику?","Were past payments paid according to schedule?")).setNegativeButton(AppPreferences.tr(this,"Нет, оставить неоплаченными","No, leave unpaid"),(d,w)->finishSaved()).setPositiveButton(AppPreferences.tr(this,"Да, отметить оплаченными","Yes, mark paid"),(d,w)->{ReminderScheduler.markPastPaid(this,id);finishSaved();}).setCancelable(false).show();}
    private void finishSaved(){requestNotificationPermissionIfNeeded();Toast.makeText(this,editReminder==null?AppPreferences.tr(this,"Запись сохранена","Saved"):AppPreferences.tr(this,"Изменения сохранены","Changes saved"),Toast.LENGTH_LONG).show();setResult(RESULT_OK);finish();}
    private void requestNotificationPermissionIfNeeded(){if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATION_PERMISSION_REQUEST);}

    private TextInputEditText addField(LinearLayout p,String h,int it){TextInputLayout l=addLayout(p,h);return addInput(l,it);} private TextInputLayout addLayout(LinearLayout p,String h){TextInputLayout l=createLayout(h);LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(-1,-2);x.setMargins(0,0,0,dp(12));p.addView(l,x);return l;} private TextInputLayout createLayout(String h){TextInputLayout l=new TextInputLayout(this);l.setHint(h);l.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);l.setBoxBackgroundColor(ContextCompat.getColor(this,R.color.card_background));l.setBoxStrokeColor(ContextCompat.getColor(this,R.color.primary));l.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.text_secondary)));l.setBoxCornerRadii(dp(14),dp(14),dp(14),dp(14));return l;} private TextInputEditText addInput(TextInputLayout l,int type){TextInputEditText e=createInput(type);l.addView(e,new TextInputLayout.LayoutParams(-1,-2));return e;} private TextInputEditText createInput(int type){TextInputEditText e=new TextInputEditText(this);e.setInputType(type);e.setSingleLine(true);e.setMinHeight(dp(58));e.setTextColor(ContextCompat.getColor(this,R.color.text_main));return e;}
    private Spinner createSpinner(){Spinner s=new Spinner(this);UiUtils.styleSpinner(this,s);return s;} private LinearLayout.LayoutParams spinnerParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(56));p.setMargins(0,0,0,dp(12));return p;} private void addLabel(LinearLayout p,String s){TextView t=text(s,14,R.color.text_secondary,true);LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(-1,-2);x.setMargins(0,0,0,dp(6));p.addView(t,x);}
    private double positive(TextInputEditText e){double v=Double.parseDouble(clean(text(e)));if(v<=0)throw new IllegalArgumentException();return v;} private double nonNeg(TextInputEditText e){String s=clean(text(e));if(s.isEmpty())return 0;double v=Double.parseDouble(s);if(v<0)throw new IllegalArgumentException();return v;} private String text(TextInputEditText e){return e.getText()==null?"":e.getText().toString();} private String clean(String s){return s.trim().replace(" ","").replace("\u00A0","").replace("\u202F","").replace(',','.');} private String format(double v){return String.format(java.util.Locale.US,"%.2f",v).replace('.',',');} private String trim(double v){return v==Math.floor(v)?String.valueOf((long)v):String.valueOf(v).replace('.',',');} private String russianDays(int v){return v==1?"день":(v>=2&&v<=4?"дня":"дней");}
    private TextView top(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setClickable(true);return t;} private TextView text(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,c));if(b)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;} private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    private static class SimpleWatcher implements TextWatcher{private final Runnable r;SimpleWatcher(Runnable r){this.r=r;}public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){}public void afterTextChanged(Editable e){if(r!=null)r.run();}}
    private static class MoneyWatcher implements TextWatcher{private final Runnable r;MoneyWatcher(Runnable r){this.r=r;}public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){}public void afterTextChanged(Editable e){if(r!=null)r.run();}}
    private static class SimpleSelected implements android.widget.AdapterView.OnItemSelectedListener{private final Runnable r;SimpleSelected(Runnable r){this.r=r;}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(r!=null)r.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}}
}
