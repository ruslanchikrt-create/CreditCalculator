package com.example.creditcalculator;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class FinancialStatsActivity extends AppCompatActivity {
    private DrawerLayout drawer;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout content;

    private static class Stats {
        double scheduledPaidAll;
        double earlyPaidAll;
        double earlyPaidActive;
        double totalPaidAll;
        double interestPaidAll;
        double interestPaidArchived;
        double futureInterestActive;
        double insuranceAll;
        double insuranceActive;
        double commissionsAll;
        double commissionsActive;
        double overpaymentAll;
        double overpaymentActive;
        double activeDebt;
        double savingsAll;
        double savingsActive;
        double savingsArchived;
        double activeDepositPrincipal;
        double archivedDepositPrincipal;
        double expectedDepositIncome;
        double archivedDepositIncome;
        int activeLoans;
        int archivedLoans;
        int activeDeposits;
        int archivedDeposits;
    }

    @Override protected void attachBaseContext(Context c){super.attachBaseContext(AppPreferences.wrapLocale(c));}

    @Override protected void onCreate(Bundle b){
        AppPreferences.applyNightMode(this);
        super.onCreate(b);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        setContentView(build());
        render();
    }

    @Override protected void onResume(){super.onResume();render();}

    private View build(){
        drawer=new DrawerLayout(this);drawer.setFitsSystemWindows(false);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);UiUtils.applyBackground(this,root);drawer.addView(root,new DrawerLayout.LayoutParams(-1,-1));

        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),0,dp(10),0);bar.setBackgroundColor(ContextCompat.getColor(this,R.color.primary));root.addView(bar,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView menu=top("☰",30);menu.setOnClickListener(v->drawer.openDrawer(GravityCompat.START));bar.addView(menu,new LinearLayout.LayoutParams(dp(54),dp(54)));
        TextView title=top(AppPreferences.tr(this,"Финансовая статистика","Financial statistics"),20);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setGravity(Gravity.CENTER_VERTICAL);bar.addView(title,new LinearLayout.LayoutParams(0,-1,1f));

        swipeRefresh=new SwipeRefreshLayout(this);swipeRefresh.setColorSchemeResources(R.color.primary);root.addView(swipeRefresh,new LinearLayout.LayoutParams(-1,0,1f));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);swipeRefresh.addView(scroll,new SwipeRefreshLayout.LayoutParams(-1,-1));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(22),dp(20),dp(34));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        swipeRefresh.setOnRefreshListener(()->{try{ReminderScheduler.refreshAll(this);render();Toast.makeText(this,AppPreferences.tr(this,"Данные обновлены","Data refreshed"),Toast.LENGTH_SHORT).show();}finally{swipeRefresh.setRefreshing(false);}});

        LinearLayout nav=NavigationPanel.build(this,drawer,NavigationPanel.PAGE_STATS);DrawerLayout.LayoutParams np=new DrawerLayout.LayoutParams(NavigationPanel.drawerWidth(this),-1);np.gravity=GravityCompat.START;drawer.addView(nav,np);
        ViewCompat.setOnApplyWindowInsetsListener(drawer,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());root.setPadding(0,bars.top,0,bars.bottom);nav.setPadding(0,bars.top,0,bars.bottom);scroll.setPadding(0,0,0,bars.bottom+dp(12));return insets;});
        return drawer;
    }

    private void render(){
        if(content==null)return;
        content.removeAllViews();
        Stats s=collect();
        content.addView(text(AppPreferences.tr(this,"Финансовая статистика","Financial statistics"),28,R.color.text_main,true));
        TextView sub=text(AppPreferences.tr(this,"Общая картина по активным и архивным кредитам, платежам и вкладам.","Overview of active and archived loans, payments and deposits."),14,R.color.text_secondary,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,dp(5),0,dp(18));content.addView(sub,sp);

        content.addView(sectionTitle(AppPreferences.tr(this,"Кредиты — за всё время","Loans — lifetime")));
        content.addView(card(new String[][]{
                {AppPreferences.tr(this,"Всего внесено по кредитам","Total paid toward loans"),FormatUtils.money(this,s.totalPaidAll),AppPreferences.tr(this,"Сумма отмеченных обычных платежей и зарегистрированных досрочных погашений по активным и архивным кредитам. Корзина не учитывается.","Sum of recorded scheduled payments and registered early repayments for active and archived loans. Trash is excluded.")},
                {AppPreferences.tr(this,"Оплачено по графику","Scheduled payments paid"),FormatUtils.money(this,s.scheduledPaidAll),AppPreferences.tr(this,"Сумма обычных платежей, которые были отмечены оплаченными за всё время.","Total normal installments recorded as paid over time.")},
                {AppPreferences.tr(this,"Досрочно погашено","Early repayments"),FormatUtils.money(this,s.earlyPaidAll),AppPreferences.tr(this,"Сумма зарегистрированных досрочных погашений по активным и архивным кредитам за всё время.","Registered early repayments across active and archived loans over time.")},
                {AppPreferences.tr(this,"Уже выплачено процентов","Interest already paid"),FormatUtils.money(this,s.interestPaidAll),AppPreferences.tr(this,"Расчётная сумма процентов, уже пришедшаяся на отмеченные оплаченные платежи активных и архивных кредитов.","Estimated interest already included in recorded paid installments of active and archived loans.")},
                {AppPreferences.tr(this,"В том числе по архивным кредитам","Including archived loans"),FormatUtils.money(this,s.interestPaidArchived),AppPreferences.tr(this,"Часть уже выплаченных процентов, относящаяся к кредитам, которые сейчас находятся в архиве.","Part of already paid interest belonging to loans currently in archive.")},
                {AppPreferences.tr(this,"Страховка по кредитам","Loan insurance"),FormatUtils.money(this,s.insuranceAll),AppPreferences.tr(this,"Сумма страховок, указанных в кредитных записях, включая страховки применённых рефинансирований. Она входит в общую переплату.","Insurance entered for loan records, including insurance from applied refinancings. It is included in total overpayment.")},
                {AppPreferences.tr(this,"Комиссии рефинансирования","Refinancing fees"),FormatUtils.money(this,s.commissionsAll),AppPreferences.tr(this,"Разовые комиссии применённых рефинансирований. Они входят в общую переплату.","One-time fees from applied refinancings. They are included in total overpayment.")},
                {AppPreferences.tr(this,"Общая переплата: проценты + страховка + комиссии","Total overpayment: interest + insurance + fees"),FormatUtils.money(this,s.overpaymentAll),AppPreferences.tr(this,"Сумма расчётных процентов за весь срок, страховок и комиссий рефинансирования по активным и архивным кредитам. Штрафы и пени сюда не включаются.","Estimated lifetime interest plus insurance and refinancing fees for active and archived loans. Penalties are excluded.")},
                {AppPreferences.tr(this,"Сэкономлено за всё время","Lifetime savings"),FormatUtils.money(this,s.savingsAll),AppPreferences.tr(this,"Расчётная экономия от зарегистрированных досрочных погашений и рефинансирования по активным и архивным кредитам. Страховка не считается сэкономленной автоматически.","Estimated savings from recorded early repayments and refinancing across active and archived loans. Insurance is not automatically treated as saved.")}
        }));

        content.addView(sectionTitle(AppPreferences.tr(this,"Активные кредиты — что впереди","Active loans — ahead")));
        content.addView(card(new String[][]{
                {AppPreferences.tr(this,"Текущий общий долг","Current total debt"),FormatUtils.money(this,s.activeDebt),AppPreferences.tr(this,"Остаток основного долга только по активным кредитам. Архив не входит.","Remaining principal for active loans only. Archive is excluded.")},
                {AppPreferences.tr(this,"Будущие проценты","Future interest"),FormatUtils.money(this,s.futureInterestActive),AppPreferences.tr(this,"Сколько расчётных процентов ещё предстоит выплатить по текущим активным графикам, если условия не менять.","Estimated interest still to be paid under current active schedules if terms do not change.")},
                {AppPreferences.tr(this,"Общая переплата активных кредитов","Active-loan total overpayment"),FormatUtils.money(this,s.overpaymentActive),AppPreferences.tr(this,"Расчётные проценты за весь срок плюс страховка и комиссии рефинансирования по кредитам, которые сейчас активны.","Estimated lifetime interest plus insurance and refinancing fees for currently active loans.")},
                {AppPreferences.tr(this,"Страховка активных кредитов","Active-loan insurance"),FormatUtils.money(this,s.insuranceActive),AppPreferences.tr(this,"Страховка учитывается в общей переплате независимо от того, была она включена в кредит или оплачена отдельно.","Insurance is included in total overpayment whether it was financed or paid separately.")},
                {AppPreferences.tr(this,"Досрочно погашено по активным","Early repaid on active loans"),FormatUtils.money(this,s.earlyPaidActive),AppPreferences.tr(this,"Все зарегистрированные досрочные погашения только по кредитам, которые сейчас активны. После переноса кредита в архив эта сумма переходит в историческую статистику.","All registered early repayments for loans that are currently active. Once a loan moves to archive, this amount moves to historical statistics.")},
                {AppPreferences.tr(this,"Выгода по активным кредитам","Savings on active loans"),FormatUtils.money(this,s.savingsActive),AppPreferences.tr(this,"Расчётная экономия по активным кредитам. Выгода архивных кредитов здесь не учитывается.","Estimated savings for active loans. Archived-loan savings are excluded.")},
                {AppPreferences.tr(this,"Активных кредитов","Active loans"),String.valueOf(s.activeLoans),AppPreferences.tr(this,"Количество активных кредитных записей без вкладов.","Number of active credit records, excluding deposits.")}
        }));

        content.addView(sectionTitle(AppPreferences.tr(this,"Архив кредитов","Loan archive")));
        content.addView(card(new String[][]{
                {AppPreferences.tr(this,"Архивных кредитов","Archived loans"),String.valueOf(s.archivedLoans),AppPreferences.tr(this,"Количество кредитных записей, находящихся в архиве.","Number of credit records currently in archive.")},
                {AppPreferences.tr(this,"Выгода архивных кредитов","Archived-loan savings"),FormatUtils.money(this,s.savingsArchived),AppPreferences.tr(this,"Историческая расчётная экономия от досрочных погашений и рефинансирования кредитов, которые уже находятся в архиве.","Historical estimated savings from early repayment and refinancing of archived loans.")}
        }));

        content.addView(sectionTitle(AppPreferences.tr(this,"Вклады","Deposits")));
        content.addView(card(new String[][]{
                {AppPreferences.tr(this,"Сейчас во вкладах","Currently deposited"),FormatUtils.money(this,s.activeDepositPrincipal),AppPreferences.tr(this,"Сумма первоначальных сумм всех активных вкладов.","Principal amount across active deposits.")},
                {AppPreferences.tr(this,"Ожидаемый доход по активным вкладам","Expected income on active deposits"),FormatUtils.money(this,s.expectedDepositIncome),AppPreferences.tr(this,"Расчётный доход, который ожидается по активным вкладам при сохранении текущих условий.","Estimated income expected from active deposits if current terms remain unchanged.")},
                {AppPreferences.tr(this,"Доход завершённых вкладов","Income from completed deposits"),FormatUtils.money(this,s.archivedDepositIncome),AppPreferences.tr(this,"Расчётный доход по вкладам, которые находятся в архиве. Это значение рассчитано по сохранённым условиям и может отличаться от фактической выплаты банка.","Estimated income for deposits in archive. It is calculated from saved terms and may differ from the bank's actual payout.")},
                {AppPreferences.tr(this,"Сумма завершённых вкладов","Completed deposit principal"),FormatUtils.money(this,s.archivedDepositPrincipal),AppPreferences.tr(this,"Сумма первоначальных сумм вкладов, которые сейчас находятся в архиве.","Principal amount of deposits currently in archive.")},
                {AppPreferences.tr(this,"Активных / завершённых вкладов","Active / completed deposits"),s.activeDeposits+" / "+s.archivedDeposits,AppPreferences.tr(this,"Количество активных вкладов и вкладов в архиве.","Number of active deposits and deposits in archive.")}
        }));

        TextView note=text("ⓘ "+AppPreferences.tr(this,"Статистика строится по данным, сохранённым в приложении. Банковские округления, комиссии, штрафы и фактический доход по вкладам могут отличаться.","Statistics are based on data saved in the app. Bank rounding, fees, penalties and actual deposit income may differ."),13,R.color.text_secondary,false);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.setMargins(0,dp(18),0,0);content.addView(note,np);
    }

    private Stats collect(){
        Stats s=new Stats();
        List<ReminderScheduler.PaymentReminder> all=ReminderScheduler.loadAll(this);
        for(ReminderScheduler.PaymentReminder r:all){
            if(ReminderScheduler.STATUS_TRASH.equals(r.status))continue;
            boolean active=ReminderScheduler.STATUS_ACTIVE.equals(r.status);
            boolean archived=ReminderScheduler.STATUS_ARCHIVE.equals(r.status);
            if(!active&&!archived)continue;
            boolean deposit=ReminderScheduler.TYPE_DEPOSIT.equals(ReminderScheduler.normalizeType(r.type));
            if(deposit){
                double income=ReminderScheduler.depositExpectedIncome(r);
                if(active){s.activeDeposits++;s.activeDepositPrincipal+=r.principal;s.expectedDepositIncome+=income;}
                else{s.archivedDeposits++;s.archivedDepositPrincipal+=r.principal;s.archivedDepositIncome+=income;}
                continue;
            }
            if(active){s.activeLoans++;s.activeDebt+=ReminderScheduler.remainingDebt(r);s.futureInterestActive+=ReminderScheduler.remainingInterest(r);s.insuranceActive+=ReminderScheduler.totalInsuranceCosts(r);s.commissionsActive+=ReminderScheduler.totalCommissionCosts(r);s.overpaymentActive+=ReminderScheduler.totalOverpayment(r);}
            else s.archivedLoans++;
            s.insuranceAll+=ReminderScheduler.totalInsuranceCosts(r);s.commissionsAll+=ReminderScheduler.totalCommissionCosts(r);s.overpaymentAll+=ReminderScheduler.totalOverpayment(r);
            double pi=ReminderScheduler.paidInterest(r);s.interestPaidAll+=pi;if(archived)s.interestPaidArchived+=pi;
            for(ReminderScheduler.InstallmentEntry e:ReminderScheduler.ledger(r))if(e!=null&&e.index>=0&&e.paidAt>0){double paid=e.paidAmount>0?e.paidAmount:ReminderScheduler.paymentAmount(r,e.index);s.scheduledPaidAll+=Math.max(0,paid);}
            for(ReminderScheduler.BenefitEvent e:ReminderScheduler.benefits(r)){
                if(ReminderScheduler.HISTORY_EARLY.equals(e.type)){double a=Math.max(0,e.actionAmount);s.earlyPaidAll+=a;if(active)s.earlyPaidActive+=a;}
            }
            double benefit=ReminderScheduler.totalBenefit(r);s.savingsAll+=benefit;if(active)s.savingsActive+=benefit;else s.savingsArchived+=benefit;
        }
        s.totalPaidAll=s.scheduledPaidAll+s.earlyPaidAll;
        return s;
    }

    private TextView sectionTitle(String s){TextView t=text(s,20,R.color.text_main,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(18),0,dp(9));t.setLayoutParams(p);return t;}

    private MaterialCardView card(String[][] rows){MaterialCardView c=new MaterialCardView(this);c.setCardBackgroundColor(ContextCompat.getColor(this,R.color.card_background));c.setRadius(dp(18));c.setStrokeColor(ContextCompat.getColor(this,R.color.border));c.setStrokeWidth(dp(1));LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(10),dp(18),dp(12));c.addView(box);for(int i=0;i<rows.length;i++){metric(box,rows[i][0],rows[i][1],rows[i][2]);if(i<rows.length-1){View d=new View(this);d.setBackgroundColor(ContextCompat.getColor(this,R.color.border));LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(-1,dp(1));dpv.setMargins(0,dp(10),0,0);box.addView(d,dpv);}}return c;}

    private void metric(LinearLayout box,String label,String value,String hint){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(10),0,0);box.addView(row,new LinearLayout.LayoutParams(-1,-2));TextView l=text(label+"  ⓘ",14,R.color.text_secondary,true);l.setClickable(true);l.setOnClickListener(v->showInfo(label,hint));row.addView(l,new LinearLayout.LayoutParams(0,-2,1f));TextView val=text(value,16,R.color.text_main,true);val.setGravity(Gravity.END);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-2,-2);vp.setMargins(dp(10),0,0,0);row.addView(val,vp);}

    private void showInfo(String title,String msg){new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton(AppPreferences.tr(this,"Понятно","OK"),null).show();}
    private TextView top(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setClickable(true);return t;}
    private TextView text(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ContextCompat.getColor(this,c));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
