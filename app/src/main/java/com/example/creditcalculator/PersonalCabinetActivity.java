package com.example.creditcalculator;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;

public class PersonalCabinetActivity extends AppCompatActivity {
    private SwipeRefreshLayout swipeRefresh;
    private ScrollView scroll;
    private TextInputEditText fullNameInput, incomeInput, obligationsInput, ratingInput, applicationsInput, rejectionsInput;
    private TextView scoreValue, disciplineValue, activeValue, archivedValue, debtValue, monthlyValue, overdueValue, dtiValue, officialValue;
    private boolean formattingMoney;
    private boolean loadingFields;

    private static class Stats {
        int activeLoans, archivedLoans, paidEvents, onTimeEvents, lateEvents, currentOverdue;
        double debt, monthlyPayments;
    }

    @Override protected void attachBaseContext(Context c){super.attachBaseContext(AppPreferences.wrapLocale(c));}

    @Override protected void onCreate(Bundle b){
        AppPreferences.applyNightMode(this);
        super.onCreate(b);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        setContentView(build());
        loadSavedFields();
        updatePreviewFromInputs();
        if(!AppPreferences.isCabinetDisclaimerAccepted(this))getWindow().getDecorView().post(this::showDisclaimer);
    }

    @Override protected void onResume(){super.onResume();updatePreviewFromInputs();}

    private View build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);UiUtils.applyBackground(this,root);
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(6),0,dp(12),0);bar.setBackgroundColor(ContextCompat.getColor(this,R.color.primary));root.addView(bar,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView back=top("‹",34);back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView title=top(AppPreferences.tr(this,"Личный кабинет","Personal cabinet"),20);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setGravity(Gravity.CENTER_VERTICAL);bar.addView(title,new LinearLayout.LayoutParams(0,-1,1f));

        swipeRefresh=new SwipeRefreshLayout(this);swipeRefresh.setColorSchemeResources(R.color.primary);root.addView(swipeRefresh,new LinearLayout.LayoutParams(-1,0,1f));
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);swipeRefresh.addView(scroll,new SwipeRefreshLayout.LayoutParams(-1,-1));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(22),dp(20),dp(34));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));

        content.addView(healthCard());
                  MaterialButton statsButton=new MaterialButton(this);statsButton.setText(AppPreferences.tr(this,"Финансовая статистика","Financial statistics"));statsButton.setAllCaps(false);statsButton.setTextSize(15);statsButton.setTextColor(ContextCompat.getColor(this,R.color.primary));statsButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.card_background)));statsButton.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));statsButton.setStrokeWidth(dp(1));statsButton.setCornerRadius(dp(14));statsButton.setOnClickListener(v->startActivity(new Intent(this,FinancialStatsActivity.class)));LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(-1,dp(54));stp.setMargins(0,dp(14),0,0);content.addView(statsButton,stp);

        TextView dataTitle=text(AppPreferences.tr(this,"Ваши данные","Your data"),22,R.color.text_main,true);LinearLayout.LayoutParams dtp=new LinearLayout.LayoutParams(-1,-2);dtp.setMargins(0,dp(22),0,dp(10));content.addView(dataTitle,dtp);
        fullNameInput=field(content,AppPreferences.tr(this,"ФИО","Full name"),AppPreferences.tr(this,"Фамилия, имя и отчество владельца профиля. Эти данные сохраняются только внутри приложения и не отправляются в БКИ или банк.","Profile owner's full name. This value is stored only in the app and is not sent to a credit bureau or bank."),"",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        incomeInput=field(content,AppPreferences.tr(this,"Ежемесячный доход","Monthly income"),AppPreferences.tr(this,"Используется только для внутренней оценки долговой нагрузки. Укажите средний доход после налогов, который реально доступен для платежей.","Used only for the internal debt-burden estimate. Enter average after-tax income actually available for payments."),"",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        obligationsInput=field(content,AppPreferences.tr(this,"Другие обязательные платежи в месяц","Other mandatory monthly payments"),AppPreferences.tr(this,"Аренда, алименты, обязательные рассрочки или другие регулярные платежи, которых нет среди записей приложения.","Rent, alimony, mandatory installments or other recurring obligations not recorded in the app."),"",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ratingInput=field(content,AppPreferences.tr(this,"Официальный рейтинг БКИ, если известен","Official bureau score, if known"),AppPreferences.tr(this,"Введите значение из вашего БКИ вручную. Оно показывается для справки и не подменяется внутренней оценкой приложения.","Enter the score from your credit bureau manually. It is shown for reference and is separate from the app's internal score."),"",InputType.TYPE_CLASS_NUMBER);
        applicationsInput=field(content,AppPreferences.tr(this,"Кредитных заявок за последние 12 месяцев","Credit applications in the last 12 months"),AppPreferences.tr(this,"Частые заявки за короткий период могут быть признаком повышенной кредитной активности. Эти данные приложение само не получает из БКИ.","Frequent applications in a short period may indicate elevated credit activity. The app does not obtain this data from a bureau."),"",InputType.TYPE_CLASS_NUMBER);
        rejectionsInput=field(content,AppPreferences.tr(this,"Отказов по кредитам за последние 12 месяцев","Credit rejections in the last 12 months"),AppPreferences.tr(this,"Укажите вручную известное количество отказов. Это один из дополнительных факторов внутренней оценки приложения.","Enter the known number of rejections manually. It is one additional factor in the app's internal estimate."),"",InputType.TYPE_CLASS_NUMBER);

        attachMoneyWatcher(incomeInput);attachMoneyWatcher(obligationsInput);
        attachPreviewWatcher(ratingInput);attachPreviewWatcher(applicationsInput);attachPreviewWatcher(rejectionsInput);

        MaterialButton save=new MaterialButton(this);save.setText(AppPreferences.tr(this,"Сохранить","Save"));save.setAllCaps(false);save.setTextSize(16);save.setTextColor(Color.WHITE);save.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.primary)));save.setCornerRadius(dp(14));save.setOnClickListener(v->save());LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(56));sp.setMargins(0,dp(16),0,dp(14));content.addView(save,sp);
        TextView note=text("ⓘ "+AppPreferences.tr(this,"«Кредитное здоровье» — внутренняя ориентировочная оценка приложения, а не официальный кредитный рейтинг БКИ и не решение банка.","Credit health is an internal estimate from the app, not an official credit-bureau score or a bank decision."),13,R.color.text_secondary,true);content.addView(note);

        swipeRefresh.setOnRefreshListener(this::refreshAllData);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());v.setPadding(0,bars.top,0,0);scroll.setPadding(0,0,0,Math.max(bars.bottom,ime.bottom)+dp(18));UiUtils.ensureFocusedFieldVisible(scroll);return insets;});
        return root;
    }

    private MaterialCardView healthCard(){
        MaterialCardView c=card();LinearLayout b=box(c);
        TextView h=infoTitle(AppPreferences.tr(this,"Кредитное здоровье","Credit health"),AppPreferences.tr(this,"Внутренняя оценка 0–100. Учитываются записанная платёжная дисциплина, текущие просрочки, долговая нагрузка при указанном доходе, количество активных кредитов, закрытые/архивные кредиты и вручную указанные заявки/отказы. Это не официальный рейтинг БКИ.","Internal 0–100 estimate based on recorded payment discipline, current overdue items, debt burden when income is entered, active loans, closed/archived loans and manually entered applications/rejections. It is not an official bureau score."));b.addView(h);
        scoreValue=text("—",30,R.color.text_main,true);LinearLayout.LayoutParams scp=new LinearLayout.LayoutParams(-1,-2);scp.setMargins(0,dp(4),0,dp(12));b.addView(scoreValue,scp);
        disciplineValue=metric(b,AppPreferences.tr(this,"Платёжная дисциплина","Payment discipline"),AppPreferences.tr(this,"Доля платежей, оплаченных не позже даты по графику. Текущие неоплаченные просрочки также ухудшают показатель.","Share of installments paid no later than their due date. Current unpaid overdue installments also reduce this metric."));
        activeValue=metric(b,AppPreferences.tr(this,"Активные кредиты","Active loans"),AppPreferences.tr(this,"Количество кредитов, ипотек, автокредитов и рассрочек в «Мои платежи». Вклады не считаются кредитами.","Number of loans, mortgages, auto loans and installments in My payments. Deposits are not loans."));
        archivedValue=metric(b,AppPreferences.tr(this,"Закрытые / архивные кредиты","Closed / archived loans"),AppPreferences.tr(this,"Кредитные записи в архиве. Их история и выгода сохраняются, но они не входят в активную сводку.","Credit items in archive. Their history and savings are preserved but excluded from the active summary."));
        debtValue=metric(b,AppPreferences.tr(this,"Текущий общий долг","Current total debt"),AppPreferences.tr(this,"Ориентировочный остаток основного долга по активным кредитным записям.","Estimated remaining principal across active credit items."));
        monthlyValue=metric(b,AppPreferences.tr(this,"Обычные платежи в месяц","Scheduled monthly payments"),AppPreferences.tr(this,"Сумма ближайших обычных платежей по активным кредитам. Досрочные погашения сюда не входят.","Sum of the next normal installments for active loans. Early repayments are excluded."));
        overdueValue=metric(b,AppPreferences.tr(this,"Текущие просрочки","Current overdue items"),AppPreferences.tr(this,"Количество активных неоплаченных платежей с уже прошедшей датой.","Number of active unpaid installments whose due date has passed."));
        dtiValue=metric(b,AppPreferences.tr(this,"Долговая нагрузка","Debt burden"),AppPreferences.tr(this,"(Обычные платежи по кредитам + другие обязательные платежи) ÷ ежемесячный доход × 100%. Это внутренняя ориентировочная оценка, не банковский ПДН.","(Normal loan payments + other mandatory payments) ÷ monthly income × 100%. This is an internal estimate, not a bank's official debt-burden calculation."));
        officialValue=metric(b,AppPreferences.tr(this,"Официальный рейтинг БКИ","Official bureau score"),AppPreferences.tr(this,"Показывается только введённое вами значение. Приложение не подключено к БКИ и не может получить официальный рейтинг самостоятельно.","Shows only the value you entered. The app is not connected to a credit bureau and cannot obtain an official score itself."));
        return c;
    }

    private TextView metric(LinearLayout box,String title,String hint){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,dp(9),0,0);box.addView(row,rp);
        TextView l=infoTitle(title,hint);l.setTextSize(14);l.setTextColor(ContextCompat.getColor(this,R.color.text_secondary));row.addView(l,new LinearLayout.LayoutParams(0,-2,1f));
        TextView value=text("—",15,R.color.text_main,true);value.setGravity(Gravity.END);row.addView(value,new LinearLayout.LayoutParams(-2,-2));return value;
    }

    private void loadSavedFields(){
        if(fullNameInput==null)return;loadingFields=true;
        fullNameInput.setText(AppPreferences.getCabinetFullName(this));
        incomeInput.setText(formatMoneyValue(AppPreferences.getMonthlyIncome(this)));
        obligationsInput.setText(formatMoneyValue(AppPreferences.getOtherObligations(this)));
        ratingInput.setText(intText(AppPreferences.getOfficialBkiRating(this)));
        applicationsInput.setText(intText(AppPreferences.getCreditApplications(this)));
        rejectionsInput.setText(intText(AppPreferences.getCreditRejections(this)));
        loadingFields=false;
    }

    private void updatePreviewFromInputs(){
        if(scoreValue==null)return;
        Stats st=collectStats();double income=parseDoubleSafe(incomeInput),other=parseDoubleSafe(obligationsInput);int rating=parseIntSafe(ratingInput),apps=parseIntSafe(applicationsInput),rejects=parseIntSafe(rejectionsInput);double dti=income>0?(st.monthlyPayments+other)/income*100d:-1;int score=creditHealth(st,dti,apps,rejects);
        scoreValue.setText(score+"/100 · "+scoreLabel(score));scoreValue.setTextColor(ContextCompat.getColor(this,score>=85?R.color.success:score>=60?R.color.warning:R.color.danger));
        disciplineValue.setText(disciplineText(st));activeValue.setText(String.valueOf(st.activeLoans));archivedValue.setText(String.valueOf(st.archivedLoans));debtValue.setText(FormatUtils.money(this,st.debt));monthlyValue.setText(FormatUtils.money(this,st.monthlyPayments));overdueValue.setText(String.valueOf(st.currentOverdue));dtiValue.setText(dti<0?AppPreferences.tr(this,"Укажите доход","Enter income"):String.format(Locale.US,"%.1f%%",dti));officialValue.setText(rating>0?String.valueOf(rating):AppPreferences.tr(this,"Не указан","Not entered"));
    }

    private void refreshAllData(){
        try{ReminderScheduler.refreshAll(this);loadSavedFields();updatePreviewFromInputs();Toast.makeText(this,AppPreferences.tr(this,"Данные обновлены","Data refreshed"),Toast.LENGTH_SHORT).show();}
        finally{if(swipeRefresh!=null)swipeRefresh.setRefreshing(false);}
    }

    private void save(){
        try{
            double income=parseDouble(incomeInput),other=parseDouble(obligationsInput);int rating=parseInt(ratingInput),apps=parseInt(applicationsInput),rejects=parseInt(rejectionsInput);if(rating<0||rating>999)throw new IllegalArgumentException();
            AppPreferences.setCabinetFullName(this,fullNameInput.getText()==null?"":fullNameInput.getText().toString());AppPreferences.setMonthlyIncome(this,income);AppPreferences.setOtherObligations(this,other);AppPreferences.setOfficialBkiRating(this,rating);AppPreferences.setCreditApplications(this,apps);AppPreferences.setCreditRejections(this,rejects);
            updatePreviewFromInputs();Toast.makeText(this,AppPreferences.tr(this,"Данные сохранены","Data saved"),Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,AppPreferences.tr(this,"Проверьте введённые значения","Check entered values"),Toast.LENGTH_SHORT).show();}
    }

    private void showDisclaimer(){
        if(isFinishing()||AppPreferences.isCabinetDisclaimerAccepted(this))return;
        new AlertDialog.Builder(this).setTitle(AppPreferences.tr(this,"Важно","Important")).setMessage(AppPreferences.tr(this,"Показатели кредитного здоровья, платёжной дисциплины и долговой нагрузки являются внутренней ориентировочной оценкой приложения. Они не являются официальным кредитным рейтингом БКИ, банковским ПДН или решением банка. Для официальных данных используйте сведения банка и вашего БКИ.","Credit health, payment discipline and debt burden are internal estimates from the app. They are not an official credit-bureau score, a bank debt-burden calculation or a bank decision. Use your bank and credit bureau for official figures.")).setCancelable(false).setPositiveButton(AppPreferences.tr(this,"Понятно, продолжить","I understand, continue"),(d,w)->AppPreferences.setCabinetDisclaimerAccepted(this,true)).show();
    }

    private Stats collectStats(){
        Stats st=new Stats();List<ReminderScheduler.PaymentReminder> all=ReminderScheduler.loadAll(this);
        for(ReminderScheduler.PaymentReminder r:all){String type=ReminderScheduler.normalizeType(r.type);if(ReminderScheduler.TYPE_DEPOSIT.equals(type)||ReminderScheduler.STATUS_TRASH.equals(r.status))continue;boolean active=ReminderScheduler.STATUS_ACTIVE.equals(r.status);boolean archived=ReminderScheduler.STATUS_ARCHIVE.equals(r.status);if(active){st.activeLoans++;st.debt+=ReminderScheduler.remainingDebt(r);int ni=ReminderScheduler.nextPaymentIndex(r);if(ni>=0)st.monthlyPayments+=ReminderScheduler.paymentAmount(r,ni);st.currentOverdue+=ReminderScheduler.overdueCount(r);}if(archived)st.archivedLoans++;for(int i=0;i<r.months;i++)if(ReminderScheduler.isPaid(r,i)){st.paidEvents++;long paid=PaymentDateMath.startOfDay(ReminderScheduler.paidAt(r,i));long due=PaymentDateMath.startOfDay(ReminderScheduler.buildDueDate(r,i).getTimeInMillis());if(paid<=due)st.onTimeEvents++;else st.lateEvents++;}}
        return st;
    }

    private int creditHealth(Stats st,double dti,int applications,int rejections){int score=100;score-=Math.min(40,st.lateEvents*8);score-=Math.min(20,st.currentOverdue*5);if(dti>=0){if(dti>70)score-=30;else if(dti>50)score-=20;else if(dti>30)score-=10;}if(st.activeLoans>5)score-=5;score-=Math.min(10,rejections*2);if(applications>5)score-=Math.min(10,(applications-5)*2);score+=Math.min(5,st.archivedLoans);return Math.max(0,Math.min(100,score));}
    private String disciplineText(Stats st){if(st.paidEvents==0&&st.currentOverdue==0)return AppPreferences.tr(this,"Нет данных","No data");int base=st.paidEvents+st.currentOverdue;double pct=base<=0?100:st.onTimeEvents*100d/base;return String.format(Locale.US,"%.0f%% · %d/%d",pct,st.onTimeEvents,base);}
    private String scoreLabel(int s){if(s>=85)return AppPreferences.tr(this,"Отлично","Excellent");if(s>=70)return AppPreferences.tr(this,"Хорошо","Good");if(s>=50)return AppPreferences.tr(this,"Средне","Fair");return AppPreferences.tr(this,"Требует внимания","Needs attention");}

    private TextInputEditText field(LinearLayout parent,String title,String hint,String value,int inputType){TextView l=infoTitle(title,hint);l.setTextSize(14);l.setTextColor(ContextCompat.getColor(this,R.color.text_secondary));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(14),0,dp(5));parent.addView(l,lp);TextInputLayout layout=new TextInputLayout(this);layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);layout.setBoxBackgroundColor(ContextCompat.getColor(this,R.color.card_background));layout.setBoxStrokeColor(ContextCompat.getColor(this,R.color.primary));layout.setBoxCornerRadii(dp(12),dp(12),dp(12),dp(12));TextInputEditText e=new TextInputEditText(this);e.setInputType(inputType);e.setSingleLine(true);e.setTextColor(ContextCompat.getColor(this,R.color.text_main));e.setText(value);layout.addView(e,new TextInputLayout.LayoutParams(-1,dp(58)));parent.addView(layout,new LinearLayout.LayoutParams(-1,-2));return e;}
    private TextView infoTitle(String title,String hint){TextView t=text(title+"  ⓘ",16,R.color.text_main,true);t.setClickable(true);t.setOnClickListener(v->showInfo(title,hint));return t;}
    private void showInfo(String title,String hint){new AlertDialog.Builder(this).setTitle(title).setMessage(hint).setPositiveButton(AppPreferences.tr(this,"Понятно","OK"),null).show();}

    private void attachPreviewWatcher(TextInputEditText e){e.addTextChangedListener(new TextWatcher(){@Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}@Override public void onTextChanged(CharSequence s,int st,int b,int c){}@Override public void afterTextChanged(Editable s){if(!loadingFields)updatePreviewFromInputs();}});}
    private void attachMoneyWatcher(TextInputEditText e){e.addTextChangedListener(new TextWatcher(){@Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}@Override public void onTextChanged(CharSequence s,int st,int b,int c){}@Override public void afterTextChanged(Editable s){if(formattingMoney||loadingFields)return;String formatted=formatMoneyText(s==null?"":s.toString());if(!formatted.equals(s==null?"":s.toString())){formattingMoney=true;e.setText(formatted);e.setSelection(formatted.length());formattingMoney=false;}updatePreviewFromInputs();}});}

    private String formatMoneyText(String raw){
        String s=raw==null?"":raw.replace("\u00A0","").replace(" ","").replace(',','.').replaceAll("[^0-9.]","");if(s.isEmpty())return "";int dot=s.indexOf('.');String ints=dot>=0?s.substring(0,dot):s;String frac=dot>=0?s.substring(dot+1).replace(".",""):"";if(ints.isEmpty())ints="0";try{long v=Long.parseLong(ints);String out=String.format(Locale.US,"%,d",v).replace(',',' ');if(dot>=0){if(frac.length()>2)frac=frac.substring(0,2);out+=","+frac;}return out;}catch(Exception e){return raw;}
    }
    private String formatMoneyValue(double v){if(v<=0)return "";String raw=Math.abs(v-Math.rint(v))<.005?String.format(Locale.US,"%.0f",v):String.format(Locale.US,"%.2f",v).replaceAll("0+$","").replaceAll("\\.$","");return formatMoneyText(raw);}
    private double parseDouble(TextInputEditText e){String s=e.getText()==null?"":e.getText().toString().trim().replace(" ","").replace("\u00A0","").replace(',','.');return s.isEmpty()?0:Math.max(0,Double.parseDouble(s));}
    private double parseDoubleSafe(TextInputEditText e){try{return parseDouble(e);}catch(Exception x){return 0;}}
    private int parseInt(TextInputEditText e){String s=e.getText()==null?"":e.getText().toString().trim().replace(" ","");return s.isEmpty()?0:Math.max(0,Integer.parseInt(s));}
    private int parseIntSafe(TextInputEditText e){try{return parseInt(e);}catch(Exception x){return 0;}}
    private String intText(int v){return v<=0?"":String.valueOf(v);}
    private MaterialCardView card(){MaterialCardView c=new MaterialCardView(this);c.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));c.setRadius(dp(18));c.setStrokeColor(ContextCompat.getColor(this,R.color.border));c.setStrokeWidth(dp(1));return c;}
    private LinearLayout box(MaterialCardView c){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(18),dp(16),dp(18),dp(16));c.addView(b);return b;}
    private TextView top(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setClickable(true);return t;}
    private TextView text(String s,int z,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,color));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
