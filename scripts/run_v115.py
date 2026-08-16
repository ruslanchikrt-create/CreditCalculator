from pathlib import Path
import re


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 occurrence, found {count}")
    return text.replace(old, new, 1)


# Version
p = Path("app/build.gradle")
s = p.read_text(encoding="utf-8")
s = replace_once(s, "versionCode 14", "versionCode 15", "versionCode")
s = replace_once(s, 'versionName "1.14"', 'versionName "1.15"', "versionName")
p.write_text(s, encoding="utf-8")

# Date-only helper: a financial action entered as a date must not depend on reminder hours/minutes.
Path("app/src/main/java/com/example/creditcalculator/PaymentDateMath.java").write_text(
'''package com.example.creditcalculator;

import java.util.Calendar;

final class PaymentDateMath {
    private PaymentDateMath() {}
    static long startOfDay(long millis) {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }
    static boolean isSameDay(long a,long b){return startOfDay(a)==startOfDay(b);}
    static boolean isOnOrAfterDay(long value,long reference){return startOfDay(value)>=startOfDay(reference);}
    static boolean isBeforeDay(long value,long reference){return startOfDay(value)<startOfDay(reference);}
}
''', encoding="utf-8")

# Make balance, number of remaining payments and remaining interest use the same calendar-day boundary.
p = Path("app/src/main/java/com/example/creditcalculator/ReminderScheduler.java")
s = p.read_text(encoding="utf-8")
pattern = re.compile(r'    public static double balanceAtDate\(PaymentReminder r,long date\)\{.*?\n    public static double segmentPaidInterest', re.S)
match = pattern.search(s)
if not match:
    raise SystemExit("Could not locate repayment date calculation block")
replacement = '''    public static double balanceAtDate(PaymentReminder r,long date){if(r==null)return 0;double b=r.principal;long now=System.currentTimeMillis();long actionDay=PaymentDateMath.startOfDay(date);for(int i=0;i<r.months;i++){PaymentParts p=paymentParts(r,i);if(PaymentDateMath.startOfDay(p.dueDate)>=actionDay)break;if(p.dueDate>now)b=Math.max(0,b-p.principalPart);else if(paidAmount(r,i)>0)b=Math.max(0,b-actualPrincipalPaid(r,i));}return Math.min(remainingDebt(r),b);}
    public static int remainingPaymentsAfterDate(PaymentReminder r,long date){int n=0;for(int i=0;i<r.months;i++)if(paymentAmount(r,i)>.005&&PaymentDateMath.isOnOrAfterDay(buildDueDate(r,i).getTimeInMillis(),date)&&!isPaid(r,i))n++;return n;}
    private static long firstDueOnOrAfter(PaymentReminder r,long date){for(int i=0;i<r.months;i++){long d=buildDueDate(r,i).getTimeInMillis();if(paymentAmount(r,i)>.005&&PaymentDateMath.isOnOrAfterDay(d,date)&&!isPaid(r,i))return d;}Calendar c=Calendar.getInstance();c.setTimeInMillis(PaymentDateMath.startOfDay(date));c.add(Calendar.MONTH,1);return c.getTimeInMillis();}
    private static int firstUnpaidIndexOnOrAfterDate(PaymentReminder r,long date){for(int i=0;i<r.months;i++)if(paymentAmount(r,i)>.005&&PaymentDateMath.isOnOrAfterDay(buildDueDate(r,i).getTimeInMillis(),date)&&!isPaid(r,i))return i;return Math.max(0,r.months-1);}
    public static double segmentPaidInterest'''
s = s[:match.start()] + replacement + s[match.end():]
s = replace_once(
    s,
    'public static double remainingInterestFromDate(PaymentReminder r,long date){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;double t=0;for(int i=0;i<r.months;i++){PaymentParts p=paymentParts(r,i);if(p.amount<=.005||p.dueDate<date)continue;t+=Math.max(0,p.interestPart-actualInterestPaid(r,i));}return Math.max(0,t);}',
    'public static double remainingInterestFromDate(PaymentReminder r,long date){if(r==null||TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;double t=0;for(int i=0;i<r.months;i++){PaymentParts p=paymentParts(r,i);if(p.amount<=.005||PaymentDateMath.isBeforeDay(p.dueDate,date))continue;t+=Math.max(0,p.interestPart-actualInterestPaid(r,i));}return Math.max(0,t);}',
    "remainingInterestFromDate")
s = replace_once(
    s,
    's.keptPayment=Math.max(.01,paymentAmount(r,Math.max(0,nextPaymentIndex(r))));',
    's.keptPayment=Math.max(.01,paymentAmount(r,firstUnpaidIndexOnOrAfterDate(r,date)));',
    "kept payment at action date")
p.write_text(s, encoding="utf-8")

# Normalize the UI-selected date and explain the same-day rule in the field hint.
p = Path("app/src/main/java/com/example/creditcalculator/EarlyRepaymentActivity.java")
s = p.read_text(encoding="utf-8")
s = replace_once(s,
    'actionDate.set(Calendar.HOUR_OF_DAY,12);actionDate.set(Calendar.MINUTE,0);setContentView(build());',
    'actionDate.set(Calendar.HOUR_OF_DAY,0);actionDate.set(Calendar.MINUTE,0);actionDate.set(Calendar.SECOND,0);actionDate.set(Calendar.MILLISECOND,0);setContentView(build());',
    "date normalization")
s = replace_once(s,
    'Укажите дату фактического досрочного платежа. От неё пересчитываются остаток, срок и проценты.',
    'Укажите дату фактического досрочного платежа. От неё пересчитываются остаток, срок и проценты. Если досрочное погашение делаете в день обычного платежа и банк уже списал этот платёж, сначала отметьте его «Оплачено» в графике. Расчёт учитывает статус оплаты, а не время на часах.',
    "date hint RU")
s = replace_once(s,
    'Choose the actual early repayment date. Balance, term and interest are recalculated from this date.',
    'Choose the actual early repayment date. Balance, term and interest are recalculated from this date. If an ordinary installment is due on the same day and the bank has already charged it, mark that installment Paid first. The calculation uses payment status, not the clock time.',
    "date hint EN")
p.write_text(s, encoding="utf-8")

# Keep the built-in instruction consistent with the calculator behavior.
p = Path("app/src/main/java/com/example/creditcalculator/InstructionActivity.java")
s = p.read_text(encoding="utf-8")
s = replace_once(s,
    'Проценты считаются помесячно от фактического остатка долга; последний платёж корректируется до точной суммы остатка, поэтому для сокращения срока нельзя просто умножать обычный платёж на количество месяцев. Можно поправить рассчитанные платёж и срок по данным банка — экономия пересчитается по введённым значениям.',
    'Проценты считаются помесячно от фактического остатка долга; последний платёж корректируется до точной суммы остатка, поэтому для сокращения срока нельзя просто умножать обычный платёж на количество месяцев. Если досрочное погашение приходится на дату обычного платежа, приложение ориентируется на отметку «Оплачено», а не на часы: если банк уже списал обычный платёж, сначала отметьте его оплаченным в графике. Можно поправить рассчитанные платёж и срок по данным банка — экономия пересчитается по введённым значениям.',
    "instruction RU")
s = replace_once(s,
    'Reducing the payment and reducing the term are compared side by side. You may edit calculated values to match bank figures.',
    'Reducing the payment and reducing the term are compared side by side. If early repayment falls on the regular due date, payment status is used rather than the clock time; mark the ordinary installment Paid first if the bank has already charged it. You may edit calculated values to match bank figures.',
    "instruction EN")
p.write_text(s, encoding="utf-8")

# Regression tests for the exact bug: 16 Aug at 09:00 and 15:33 are the same financial date.
Path("app/src/test/java/com/example/creditcalculator/PaymentDateMathTest.java").write_text(
'''package com.example.creditcalculator;
import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;
public class PaymentDateMathTest {
    private static long at(int y,int m,int d,int h,int min){Calendar c=Calendar.getInstance();c.clear();c.set(y,m,d,h,min,0);return c.getTimeInMillis();}
    @Test public void sameCalendarDayIgnoresClockTime(){long due=at(2026,Calendar.AUGUST,16,9,0),action=at(2026,Calendar.AUGUST,16,15,33);assertTrue(PaymentDateMath.isSameDay(due,action));assertTrue(PaymentDateMath.isOnOrAfterDay(due,action));assertFalse(PaymentDateMath.isBeforeDay(due,action));}
    @Test public void adjacentDaysRemainOrdered(){long action=at(2026,Calendar.AUGUST,16,12,0);assertTrue(PaymentDateMath.isBeforeDay(at(2026,Calendar.AUGUST,15,23,59),action));assertTrue(PaymentDateMath.isOnOrAfterDay(at(2026,Calendar.AUGUST,17,0,1),action));}
}
''', encoding="utf-8")

p = Path("app/src/test/java/com/example/creditcalculator/FinanceMathTest.java")
s = p.read_text(encoding="utf-8")
marker = '''    @Test public void deposits(){
        near(24000,FinanceMath.simpleDepositInterest(100000,24,12),0.01);
        near(126973.464,FinanceMath.monthlyCompoundDepositFinal(100000,24,12),0.1);
    }
}'''
extra = '''    @Test public void deposits(){
        near(24000,FinanceMath.simpleDepositInterest(100000,24,12),0.01);
        near(126973.464,FinanceMath.monthlyCompoundDepositFinal(100000,24,12),0.1);
    }
    @Test public void reportedSameDayCaseHasPositiveSavings(){
        double balance=325000,prepay=1000,rate=25,currentPayment=12921.943417908074;int months=36;
        double oldInterest=FinanceMath.fixedPaymentInterest(balance,months,rate,currentPayment),newBalance=balance-prepay;
        double reducedPayment=FinanceMath.annuityPayment(newBalance,months,rate);
        double reducedPaymentInterest=FinanceMath.fixedPaymentInterest(newBalance,months,rate,reducedPayment);
        int shorter=FinanceMath.monthsToPayoff(newBalance,rate,currentPayment,1200);
        double reducedTermInterest=FinanceMath.fixedPaymentInterest(newBalance,shorter,rate,currentPayment);
        near(12882.183592,reducedPayment,0.02);
        assertTrue(reducedPayment<currentPayment);
        assertEquals(36,shorter);
        assertTrue(oldInterest-reducedPaymentInterest>0);
        assertTrue(oldInterest-reducedTermInterest>0);
        assertTrue(reducedTermInterest<reducedPaymentInterest);
    }
}'''
s = replace_once(s, marker, extra, "regression test")
p.write_text(s, encoding="utf-8")
